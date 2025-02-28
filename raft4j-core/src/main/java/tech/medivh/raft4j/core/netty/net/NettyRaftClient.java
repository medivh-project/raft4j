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
import tech.medivh.raft4j.core.RaftNode;
import tech.medivh.raft4j.core.netty.RaftClientConfig;
import tech.medivh.raft4j.core.netty.exception.MessageTimeoutException;
import tech.medivh.raft4j.core.netty.message.RaftMessageCodec;
import tech.medivh.raft4j.core.netty.message.ResponseCode;
import tech.medivh.raft4j.core.netty.processor.NettyMessageProcessor;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

    private final ConcurrentMap<NodeInfo, ChannelWrapper> channelTables = new ConcurrentHashMap<>();

    private final RaftClientConfig config;

    private final EventLoopGroup selectorEventLoopGroup;

    private ExecutorService processorExecutor;

    public NettyRaftClient(RaftClientConfig config) {
        this.config = config;
        this.selectorEventLoopGroup = new NioEventLoopGroup(1, new SelectorThreadFactory());
        this.processorExecutor = Executors.newFixedThreadPool(config.getProcessThreadNum());
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
    public void stop() {
    }

    private void retryConnect() {
        int delay = 3;
//        log.warn("client connect server failed: {}:{}, retry connect after {} seconds", serverNode.getHost(),
//                serverNode.getPort(), delay);
//        CLIENT_GROUP.schedule(this::start, delay, java.util.concurrent.TimeUnit.SECONDS);
    }


    @Override
    public void registerMessageProcessor(int code, NettyMessageProcessor processor) {
        processorTable.put(code, processor);
    }

    @Override
    public ResponseFuture sendRequestSync(RaftNode node, RaftMessage request, long timeoutMillis) throws MessageTimeoutException, InterruptedException {
        Channel channel = null;
        RemoteResponseFuture responseFuture = new RemoteResponseFuture(request);
        inFlightRequests.put(request.getCode(), responseFuture);
        responseFuture.waitResponse(timeoutMillis);
        return responseFuture;
    }

    @Override
    public ResponseFuture sendRequestAsync(RaftNode node, RaftMessage request, ResponseCallback callback) {
        return null;
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
            System.out.println("active");
            log.info("netty client pipeline: channelActive");
            super.channelActive(ctx);
        }

        @Override
        public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
            System.out.println("dis connected");
            super.disconnect(ctx, promise);
        }

        @Override
        public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
            super.close(ctx, promise);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            System.out.println("inactive");
            super.channelInactive(ctx);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            System.out.println("error:" + cause.getMessage());
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
        private ChannelFuture channelFuture;
        // only affected by sync or async request, oneway is not included.
        private ChannelFuture channelToClose;
        private long lastResponseTime;
        private final String channelAddress;

        public ChannelWrapper(String address, ChannelFuture channelFuture) {
            this.lock = new ReentrantReadWriteLock();
            this.channelFuture = channelFuture;
            this.lastResponseTime = System.currentTimeMillis();
            this.channelAddress = address;
        }

        public boolean isActive() {
            return getChannel() != null && getChannel().isActive();
        }

        public boolean isWritable() {
            return getChannel().isWritable();
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

        public long getLastResponseTime() {
            return this.lastResponseTime;
        }

        public void updateLastResponseTime() {
            this.lastResponseTime = System.currentTimeMillis();
        }

        public String getChannelAddress() {
            return channelAddress;
        }

        public boolean reconnect(Channel channel) {
            if (!isWrapperOf(channel)) {
                log.warn("channelWrapper has reconnect, so do nothing, now channelId={}, input channelId={}",
                        getChannel().id(), channel.id());
                return false;
            }
            if (lock.writeLock().tryLock()) {
                try {
                    if (isWrapperOf(channel)) {
                        channelToClose = channelFuture;
//                        String[] hostAndPort = getHostAndPort(channelAddress);
//                        channelFuture = fetchBootstrap(channelAddress)
//                                .connect(hostAndPort[0], Integer.parseInt(hostAndPort[1]));
                        return true;
                    } else {
                        log.warn("channelWrapper has reconnect, so do nothing, now channelId={}, input " +
                                "channelId={}", getChannel().id(), channel.id());
                    }
                } finally {
                    lock.writeLock().unlock();
                }
            } else {
                log.warn("channelWrapper reconnect try lock fail, now channelId={}", getChannel().id());
            }
            return false;
        }

        public boolean tryClose(Channel channel) {
            try {
                lock.readLock().lock();
                if (channelFuture != null) {
                    if (channelFuture.channel().equals(channel)) {
                        return true;
                    }
                }
            } finally {
                lock.readLock().unlock();
            }
            return false;
        }

        public void close() {
//            try {
//                lock.writeLock().lock();
//                if (channelFuture != null) {
//                    closeChannel(channelFuture.channel());
//                }
//                if (channelToClose != null) {
//                    closeChannel(channelToClose.channel());
//                }
//            } finally {
//                lock.writeLock().unlock();
//            }
        }
    }


}
