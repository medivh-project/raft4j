package tech.medivh.raft4j.core.netty.net;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import tech.medivh.raft4j.core.NodeInfo;
import tech.medivh.raft4j.core.netty.NodeClientChannelInitializer;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author gongxuanzhangmelt@gmail.com
 **/
@Slf4j
public class RaftClient {

    private static final EventLoopGroup CLIENT_GROUP = new NioEventLoopGroup();

    private final NodeInfo serverNode;

    private Channel channel;
    
    public RaftClient(NodeInfo serverNode) {
        this.serverNode = serverNode;
    }

    public void connect() {
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(CLIENT_GROUP)
                    .channel(NioSocketChannel.class)
                    .handler(new NodeClientChannelInitializer());
            ChannelFuture channelFuture = bootstrap.connect(serverNode.getHost(), serverNode.getPort()).sync();
            log.info("client connected server: {}:{}", serverNode.getHost(), serverNode.getPort());
            channel = channelFuture.channel();
            channelFuture.channel().closeFuture().addListener((f) -> {
                channel = null;
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Connection interrupted, closing client...");
        }
    }


    public boolean isAlive() {
        return channel != null && channel.isActive();
    }
    
    public void sendMessage(String message) {
        if (!isAlive()) {
            log.warn("client is not alive, message: {} will not be sent", message);
            return;
        }
        channel.writeAndFlush(message);
    }

    public static void main(String[] args) {
        RaftClient localhost = new RaftClient(new NodeInfo("localhost", 8888));
        localhost.connect();
        System.out.println("client connected");
        localhost.sendMessage("hello");
    }
}
