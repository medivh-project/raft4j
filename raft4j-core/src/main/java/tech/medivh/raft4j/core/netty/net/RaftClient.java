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

/**
 * @author gongxuanzhangmelt@gmail.com 
 **/
@Slf4j
public class RaftClient {

    private static final EventLoopGroup CLIENT_GROUP = new NioEventLoopGroup();

    private final String host;
    private final int port;

    public RaftClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() {
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

            ChannelFuture channelFuture = bootstrap.connect(host, port).sync();
            log.info("client connected server: {}:{}", host, port);

            Channel channel = channelFuture.channel();
            channel.writeAndFlush("Hello, Server!").sync(); 

            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
