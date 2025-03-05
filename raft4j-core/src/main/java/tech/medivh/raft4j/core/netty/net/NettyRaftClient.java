package tech.medivh.raft4j.core.netty.net;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;
import tech.medivh.raft4j.core.NodeInfo;
import tech.medivh.raft4j.core.RaftMessage;
import tech.medivh.raft4j.core.netty.OnceExecutor;
import tech.medivh.raft4j.core.netty.RaftClientConfig;
import tech.medivh.raft4j.core.netty.exception.ConnectionException;
import tech.medivh.raft4j.core.netty.exception.MessageException;
import tech.medivh.raft4j.core.netty.exception.MessageTimeoutException;
import tech.medivh.raft4j.core.netty.exception.RequestTooMuchException;
import tech.medivh.raft4j.core.netty.exception.SendMessageException;
import tech.medivh.raft4j.core.netty.message.RaftMessageCodec;
import tech.medivh.raft4j.core.netty.message.ResponseCode;
import tech.medivh.raft4j.core.netty.processor.NettyMessageProcessor;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * hold all the client sessions
 *
 * @author gongxuanzhangmelt@gmail.com
 **/
@Slf4j
public class NettyRaftClient implements NettyMessageProcessor, RaftClient {

    private final Bootstrap bootstrap = new Bootstrap();

    private final Map<Integer/*message code*/, NettyMessageProcessor> processorTable = new HashMap<>(64);

    private final Map<Integer, ResponseFuture> inFlightRequests = new ConcurrentHashMap<>(256);

    private final Map<NodeInfo, ChannelWrapper> channelTable = new ConcurrentHashMap<>();

    private final Map<Channel, ChannelWrapper> channelWrapperTable = new ConcurrentHashMap<>();

    private static final long LOCK_TIMEOUT_MILLIS = 3000;

    private final Lock channelTableLock = new ReentrantLock();

    private final RaftClientConfig config;

    private final EventLoopGroup selectorEventLoopGroup;

    private ExecutorService processorExecutor;

    private Semaphore limitRequestSemaphore;

    public NettyRaftClient(RaftClientConfig config) {
        this.config = config;
        this.selectorEventLoopGroup = new NioEventLoopGroup(1, new SelectorThreadFactory());
        this.processorExecutor = Executors.newFixedThreadPool(config.getProcessThreadNum());
        limitRequestSemaphore = new Semaphore(config.getInflightLimitNum(), true);
    }

    @Override
    public void start() {
        this.bootstrap.group(selectorEventLoopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, false)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(
                                new RaftMessageCodec(),
                                new NettyConnectManageHandler(),
                                new ClientHandler());
                    }
                });
    }

    @Override
    public void shutdown() {
        this.channelTable.values().forEach(ChannelWrapper::close);
        this.channelTable.clear();
        this.channelWrapperTable.clear();
        this.selectorEventLoopGroup.shutdownGracefully();
        this.processorExecutor.shutdown();
    }


    @Override
    public void registerMessageProcessor(int code, NettyMessageProcessor processor) {
        processorTable.put(code, processor);
    }

    @Override
    public CompletableFuture<ResponseFuture> sendRequest(NodeInfo node, RaftMessage request, long timeoutMillis) {
        CompletableFuture<ResponseFuture> completableFuture = new CompletableFuture<>();
        long startTime = System.currentTimeMillis();
        Channel channel = createOrGetChannelSync(node);
        if (channel == null) {
            completableFuture.completeExceptionally(new ConnectionException(node));
            return completableFuture;
        }
        return sendRequest0(node, channel, request, System.currentTimeMillis() - startTime);
    }


    private CompletableFuture<ResponseFuture> sendRequest0(NodeInfo node, Channel channel, RaftMessage request,
                                                           long timeoutMillis) {
        CompletableFuture<ResponseFuture> completableFuture = new CompletableFuture<>();
        long startTime = System.currentTimeMillis();
        boolean limit;
        try {
            limit = !limitRequestSemaphore.tryAcquire(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            completableFuture.completeExceptionally(e);
            Thread.currentThread().interrupt();
            return completableFuture;
        }
        if (limit) {
            completableFuture.completeExceptionally(new RequestTooMuchException(node, config.getInflightLimitNum()));
            return completableFuture;
        }
        long remainMill = timeoutMillis - (System.currentTimeMillis() - startTime);
        if (remainMill <= 0) {
            completableFuture.completeExceptionally(new MessageTimeoutException(node, timeoutMillis));
            limitRequestSemaphore.release();
            return completableFuture;
        }
        //  support lambda
        AtomicReference<ResponseFuture> responseFutureRef = new AtomicReference<>();
        ResponseFuture responseFuture = new RemoteResponseFuture(request, node, remainMill, new ResponseCallback() {

            @Override
            public void onResponse(RaftMessage response) {
                completableFuture.complete(responseFutureRef.get());
            }

            @Override
            public void onException(Throwable throwable) {
                completableFuture.completeExceptionally(throwable);
            }
        }, new OnceExecutor(limitRequestSemaphore::release));
        responseFutureRef.set(responseFuture);
        this.inFlightRequests.put(request.getRequestId(), responseFuture);
        try {
            channel.writeAndFlush(request).addListener(f -> {
                if (f.isSuccess()) {
                    responseFuture.setSendRequestSuccess();
                    return;
                }
                requestFail(request.getRequestId());
                log.warn("send a request command to channel <{}>, channelId={}, failed.", node.addr(),
                        channel.id());
            });
        } catch (Exception e) {
            this.inFlightRequests.remove(request.getRequestId());
            completableFuture.completeExceptionally(new SendMessageException(node, e));
            log.error("send request error", e);
        }
        return completableFuture;
    }

    /**
     * sync channel return channelFuture
     **/
    private ChannelFuture createOrGetChannelAsync(NodeInfo node) {
        ChannelWrapper channelWrapper = tryGetWrapper(node);
        if (channelWrapper != null) {
            return channelWrapper.getChannelFuture();
        }
        try {
            if (channelTableLock.tryLock(LOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                channelWrapper = tryGetWrapper(node);
                if (channelWrapper == null) {
                    return createChannel(node).getChannelFuture();
                }
                //  channel future not done means connecting,it is not an illegal state
                if (channelWrapper.isActive() || !channelWrapper.getChannelFuture().isDone()) {
                    return channelWrapper.getChannelFuture();
                }
                //  illegal state 
                channelTable.remove(node);
                return createChannel(node).getChannelFuture();
            }
            log.warn("createChannel: try to lock channel table, but timeout, {}ms", LOCK_TIMEOUT_MILLIS);
        } catch (Exception e) {
            log.error("createChannel: create channel exception", e);
        } finally {
            channelTableLock.unlock();
        }
        return null;
    }

    private ChannelWrapper tryGetWrapper(NodeInfo node) {
        ChannelWrapper channelWrapper = channelTable.get(node);
        if (channelWrapper != null && channelWrapper.isActive()) {
            return channelWrapper;
        }
        return null;
    }


    private ChannelWrapper createChannel(NodeInfo node) {
        ChannelFuture connectFuture = this.bootstrap.connect(node.getHost(), node.getPort());
        log.info("createChannel: begin to connect remote host[{}] asynchronously", connectFuture);
        ChannelWrapper cw = new ChannelWrapper(node, connectFuture);
        this.channelTable.put(node, cw);
        this.channelWrapperTable.put(connectFuture.channel(), cw);
        return cw;
    }

    private Channel createOrGetChannelSync(NodeInfo node) {
        ChannelFuture channelFuture = createOrGetChannelAsync(node);
        if (channelFuture != null) {
            return channelFuture.awaitUninterruptibly().channel();
        }
        return null;
    }

    private void closeChannel(NodeInfo nodeInfo, Channel channel) {
        if (channel == null) {
            return;
        }
        try {
            if (!channelTableLock.tryLock(LOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                log.warn("closeChannel: try to lock channel table, but timeout, {}ms", LOCK_TIMEOUT_MILLIS);
                return;
            }
            final ChannelWrapper prevCW = this.channelTable.get(nodeInfo);
            log.info("closeChannel: begin close the channel[node={}, id={}] Found: {}", nodeInfo, channel.id(),
                    prevCW != null);
            //  if channel is not the same as the channel in the channel table, remove it.
            // The removal current channel behavior should occur during the Netty lifecycle.
            if (prevCW != null && prevCW.getChannel() != channel) {
                ChannelWrapper channelWrapper = channelWrapperTable.remove(channel);
                if (channelWrapper != null && channelWrapper.getChannel() == channel) {
                    this.channelTable.remove(nodeInfo);
                }
            }
            finalCloseChannel(channel);
        } catch (Exception e) {
            log.error("closeChannel: close channel exception", e);
        } finally {
            channelTableLock.unlock();
        }
    }

    private void finalCloseChannel(Channel channel) {
        channel.close().addListener(f -> {
            log.info("closeChannel: close the channel[node={}, id={}] result: {}", channel.remoteAddress(),
                    channel.id(),
                    f.isSuccess());
        });
    }

    private void closeChannel(Channel channel) {
        if (channel == null) {
            return;
        }
        try {
            if (!channelTableLock.tryLock(LOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                log.warn("closeChannel: try to lock channel table , but timeout, {}ms", LOCK_TIMEOUT_MILLIS);
                return;
            }
            if (this.channelTable.values().stream().noneMatch(cw -> cw.getChannel() == channel)) {
                log.info("closeChannel: the channel was removed from channel table before");
                return;
            }
            ChannelWrapper cw = this.channelWrapperTable.remove(channel);
            if (cw != null && cw.getChannel() == channel) {
                this.channelTable.remove(cw.nodeInfo);
                log.info("closeChannel: the channel was removed from channel table");
                finalCloseChannel(channel);
            }
        } catch (Exception e) {
            log.error("closeChannel: close channel exception", e);
        } finally {
            channelTableLock.unlock();
        }
    }

    @Override
    public void sendRequestAsync(NodeInfo node, RaftMessage request, long timeoutMillis, ResponseCallback callback) {
        long startTime = System.currentTimeMillis();
        ChannelFuture channelFuture = createOrGetChannelAsync(node);
        if (channelFuture == null) {
            callback.onException(new SendMessageException(node));
            return;
        }
        channelFuture.addListener(f -> {
            if (!f.isSuccess()) {
                callback.onException(new ConnectionException(node));
                return;
            }
            Channel channel = channelFuture.channel();
            if (channel == null || !channel.isActive()) {
                this.closeChannel(node, channel);
                callback.onException(new ConnectionException(node));
                return;
            }
            long remain = timeoutMillis - (System.currentTimeMillis() - startTime);
            if (remain <= 0) {
                callback.onException(new MessageTimeoutException(node, timeoutMillis));
                return;
            }
            sendRequest0(node, channel, request, remain).whenComplete((responseFuture, error) -> {
                if (error != null) {
                    callback.onException(error);
                } else {
                    callback.onResponse(responseFuture.getResponse());
                }
            });
        });
    }


    @Override
    public RaftMessage sendRequestSync(NodeInfo node, RaftMessage request, long timeoutMillis) throws MessageException, InterruptedException {
        try {
            return sendRequest(node, request, timeoutMillis)
                    .thenApply(ResponseFuture::getResponse)
                    .get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            throw new SendMessageException(node, e);
        } catch (TimeoutException e) {
            throw new MessageTimeoutException(node, timeoutMillis);
        }
    }


    @Override
    public void processRequest(ChannelHandlerContext ctx, RaftMessage request) {
        NettyMessageProcessor processor = this.processorTable.get(request.getCode());
        if (processor == null) {
            String notSupport = "unsupported request code " + request.getCode();
            RaftMessage response = RaftMessage.createResponse(ResponseCode.NOT_SUPPORT, notSupport);
            writeResponse(ctx.channel(), request, response);
            return;
        }
        this.processorExecutor.submit(() -> {
            try {
                processor.processRequest(ctx, request);
            } catch (Exception e) {
                log.error("process request error,request : {}", request, e);
                RaftMessage errorResp = RaftMessage.createResponse(ResponseCode.SYSTEM_ERROR, e.getMessage());
                writeResponse(ctx.channel(), request, errorResp);
            }
        });
    }

    @Override
    public void processResponse(ChannelHandlerContext ctx, RaftMessage response) {
        ResponseFuture responseFuture = inFlightRequests.remove(response.getCode());
        if (responseFuture == null) {
            log.warn("response future not found for response: {},address:{}, channel: {}", response,
                    ctx.channel().remoteAddress(), ctx.channel());
            return;
        }
        responseFuture.setResponse(response);
    }


    private void writeResponse(Channel channel, RaftMessage request, RaftMessage response) {
        response.setRequestId(request.getRequestId());
        channel.writeAndFlush(response).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                log.debug("Response[request code: {}, response code: {}, requestId: {}] is written to channel{}",
                        request.getCode(), response.getCode(), response.getRequestId(), channel);
            } else {
                log.error("Failed to write response[request code: {}, response code: {}, requestId: {}] to channel{}",
                        request.getCode(), response.getCode(), response.getRequestId(), channel, future.cause());
            }
        });
    }

    private void requestFail(int requestId) {
        ResponseFuture responseFuture = inFlightRequests.remove(requestId);
        if (responseFuture != null) {
            responseFuture.setResponse(null);
            try {
                responseFuture.executeCallback();
            } catch (Throwable e) {
                log.warn("execute callback in requestFail, and callback throw", e);
            }
        }
    }

    class NettyConnectManageHandler extends ChannelDuplexHandler {
        @Override
        public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress,
                            ChannelPromise promise) throws Exception {
            final String local = localAddress == null ? "UNKNOWN" : localAddress.toString();
            final String remote = remoteAddress == null ? "UNKNOWN" : remoteAddress.toString();
            log.info("netty client pipeline: connect  {} => {}", local, remote);
            super.connect(ctx, remoteAddress, localAddress, promise);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            log.info("netty client pipeline: channelActive {}", ctx.channel().remoteAddress());
            super.channelActive(ctx);
        }

        @Override
        public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
            log.info("netty client disconnected {}", ctx.channel().remoteAddress());
            closeChannel(ctx.channel());
            super.disconnect(ctx, promise);
        }

        @Override
        public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
            log.info("netty client close {}", ctx.channel().remoteAddress());
            closeChannel(ctx.channel());
            super.close(ctx, promise);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            log.info("netty client channelInactive {}", ctx.channel().remoteAddress());
            closeChannel(ctx.channel());
            super.channelInactive(ctx);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            log.warn("netty client exceptionCaught {}", ctx.channel().remoteAddress(), cause);
            closeChannel(ctx.channel());
        }
    }

    private class ClientHandler extends SimpleChannelInboundHandler<RaftMessage> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, RaftMessage msg) throws Exception {
            if (msg.getCode() >= ResponseCode.RESPONSE_FLAG) {
                processResponse(ctx, msg);
                return;
            }
            processRequest(ctx, msg);
        }
    }


    private final class SelectorThreadFactory extends DefaultThreadFactory {

        public SelectorThreadFactory() {
            super("raft_client_selector_");
        }
    }

    class ChannelWrapper {
        private final ReentrantReadWriteLock lock;
        private final ChannelFuture channelFuture;
        private final NodeInfo nodeInfo;

        public ChannelWrapper(NodeInfo node, ChannelFuture channelFuture) {
            this.lock = new ReentrantReadWriteLock();
            this.channelFuture = channelFuture;
            this.nodeInfo = node;
        }

        public boolean isActive() {
            return getChannel() != null && getChannel().isActive();
        }

        public boolean isWrapperOf(Channel channel) {
            return this.channelFuture.channel() != null && this.channelFuture.channel() == channel;
        }

        private Channel getChannel() {
            return getChannelFuture().channel();
        }

        public ChannelFuture getChannelFuture() {
            lock.readLock().lock();
            try {
                return this.channelFuture;
            } finally {
                lock.readLock().unlock();
            }
        }

        public void close() {
            try {
                lock.writeLock().lock();
                if (channelFuture != null) {
                    closeChannel(channelFuture.channel());
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
    }


}
