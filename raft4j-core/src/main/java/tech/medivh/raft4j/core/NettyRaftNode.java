package tech.medivh.raft4j.core;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.util.List;

/**
 * @author gongxuanzhangmelt@gmail.com
 **/
public class NettyRaftNode implements RaftNode {

    private int port = 8888;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ChannelFuture channelFuture;
    private Thread nodeServerThread;
    private NodeState nodeState = NodeState.FOLLOWER;

    @Override
    public void start() {
        bossGroup = new NioEventLoopGroup(1);  // 处理客户端连接请求
        workerGroup = new NioEventLoopGroup(); // 处理客户端的读写操作
        nodeServerThread = new Thread(this::start0, "raft-node-server");
        nodeServerThread.start();
    }

    private void start0() {
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new StringDecoder());  // 解码器
                            pipeline.addLast(new StringEncoder());  // 编码器
                            pipeline.addLast(new SimpleChannelInboundHandler<String>() {
                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
                                    System.out.println("收到消息: " + msg);
                                    ctx.writeAndFlush("你好，客户端！\n");  // 发送响应
                                }
                            });
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            // 绑定端口并启动服务器
            channelFuture = serverBootstrap.bind(port).sync();
            System.out.println("服务器启动，监听端口: " + port);

            // 等待直到服务器套接字关闭
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        if (channelFuture != null) {
            channelFuture.channel().close();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        System.out.println("服务器已停止");
    }

    @Override
    public NodeState getNodeState() {
        return null;
    }

    @Override
    public List<NodeInfo> getClusterNodes() {
        return List.of();
    }
}
