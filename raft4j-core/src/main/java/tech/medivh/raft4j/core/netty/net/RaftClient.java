package tech.medivh.raft4j.core.netty.net;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import tech.medivh.raft4j.core.NodeInfo;
import tech.medivh.raft4j.core.netty.message.RaftMessage;
import tech.medivh.raft4j.core.netty.message.RaftMessageCodec;
import tech.medivh.raft4j.core.netty.message.ResponseCode;
import tech.medivh.raft4j.core.netty.processor.NettyMessageProcessor;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * @author gongxuanzhangmelt@gmail.com
 **/
@Slf4j
public class RaftClient implements NettyMessageProcessor {

    private static final EventLoopGroup CLIENT_GROUP = new NioEventLoopGroup();

    private final Map<Integer/*message code*/, NettyMessageProcessor> processorTable = new HashMap<>(64);

    private final Map<Integer, ResponseFuture> inFlightRequests = new ConcurrentHashMap<>(256);

    private ExecutorService processorExecutor =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private final NodeInfo serverNode;

    private Channel channel;

    public RaftClient(NodeInfo serverNode) {
        this.serverNode = serverNode;
    }

    public void connect() {
        if (isAlive()) {
            return;
        }
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(CLIENT_GROUP)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(
                                    new RaftMessageCodec(),
                                    new NettyConnectManageHandler(),
                                    new ClientHandler());
                        }
                    });
            ChannelFuture channelFuture = bootstrap.connect(serverNode.getHost(), serverNode.getPort()).sync();
            channel = channelFuture.channel();
            channel.closeFuture().addListener(f -> channel = null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            retryConnect();
        } catch (Exception e) {
            retryConnect();
        }
    }

    private void retryConnect() {
        int delay = 3;
        log.warn("client connect server failed: {}:{}, retry connect after {} seconds", serverNode.getHost(),
                serverNode.getPort(), delay);
        CLIENT_GROUP.schedule(this::connect, delay, java.util.concurrent.TimeUnit.SECONDS);
    }


    public boolean isAlive() {
        return channel != null && channel.isActive();
    }

    public ResponseFuture request(RaftMessage request) {
        if (!isAlive()) {
            log.warn("client is not alive, request: {} will not be sent", request);
            return new LocalResponseFuture();
        }
        channel.writeAndFlush(request);
        return new RemoteResponseFuture();
    }

    @Override
    public void processRequest(ChannelHandlerContext ctx, RaftMessage request) {

    }

    @Override
    public void processResponse(ChannelHandlerContext ctx, RaftMessage response) {
        ResponseFuture responseFuture = inFlightRequests.remove(response.getCode());
        if (responseFuture == null) {
            log.warn("response future not found for response: {},address:{}, channel: {}", response,
                    ctx.channel().remoteAddress(), ctx.channel());
            return;
        }
    }


    static class NettyConnectManageHandler extends ChannelDuplexHandler {
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

    class ClientHandler extends SimpleChannelInboundHandler<RaftMessage> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, RaftMessage msg) throws Exception {
            if (msg.getCode() >= ResponseCode.RESPONSE_FLAG) {
                processResponse(ctx, msg);
                return;
            }
            processRequest(ctx, msg);
        }
    }


}
