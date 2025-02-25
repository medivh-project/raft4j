package tech.medivh.raft4j.core.netty.net;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import lombok.extern.slf4j.Slf4j;
import tech.medivh.raft4j.core.NodeInfo;

/**
 * @author gongxuanzhangmelt@gmail.com
 **/
@Slf4j
public class RaftClient {

    private static final EventLoopGroup CLIENT_GROUP = new NioEventLoopGroup();

    private final NodeInfo serverNode;

    public RaftClient(NodeInfo serverNode) {
        this.serverNode = serverNode;
    }

    public void connect() {
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(CLIENT_GROUP)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    .addLast(new StringEncoder())
                                    .addLast(new StringDecoder())
                                    .addLast(new SimpleChannelInboundHandler<String>() {
                                        @Override
                                        protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
                                            System.out.println("服务器响应: " + msg);
                                        }
                                    });
                        }
                    });

            ChannelFuture channelFuture = bootstrap.connect(serverNode.getHost(), serverNode.getPort()).sync();
            log.info("client connected server: {}:{}", serverNode.getHost(), serverNode.getPort());

            Channel channel = channelFuture.channel();
            channel.writeAndFlush("Hello, Server!").sync();

            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
