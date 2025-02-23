package tech.medivh.raft4j.core;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import tech.medivh.raft4j.core.netty.net.RaftServer;

import java.util.List;

/**
 * @author gongxuanzhangmelt@gmail.com
 **/
public class NettyRaftNode implements RaftNode {

    private RaftServer server = new RaftServer();

    private int port = 8888;
    private NodeState nodeState = NodeState.FOLLOWER;

    @Override
    public void start() {
        server.start();
    }

    private void start0() {
        
    }

    @Override
    public void stop() {
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
