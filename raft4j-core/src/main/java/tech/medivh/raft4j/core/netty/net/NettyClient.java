package tech.medivh.raft4j.core.netty.net;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class NettyClient {
    
    private static final  EventLoopGroup CLIENT_GROUP = new NioEventLoopGroup();

    private final String host;
    private final int port;

    public NettyClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() {
        try {
            Bootstrap bootstrap = new Bootstrap();  // 启动客户端
            bootstrap.group(CLIENT_GROUP)
                    .channel(NioSocketChannel.class)  // 使用 NIO 的 SocketChannel
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            // 客户端的处理逻辑
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new SimpleChannelInboundHandler<String>() {
                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
                                    System.out.println("服务器响应: " + msg);
                                }
                            });
                        }
                    });

            // 连接到服务器
            ChannelFuture channelFuture = bootstrap.connect(host, port).sync();
            System.out.println("客户端已连接到服务器: " + host + ":" + port);

            // 发送消息给服务器
            Channel channel = channelFuture.channel();
            channel.writeAndFlush("Hello, Server!").sync();  // 发送消息

            // 等待直到连接关闭
            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
