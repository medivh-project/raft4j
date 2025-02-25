package tech.medivh.raft4j.core;

import tech.medivh.raft4j.core.netty.WatchDog;
import tech.medivh.raft4j.core.netty.net.RaftServer;

import java.util.List;

/**
 * @author gongxuanzhangmelt@gmail.com
 **/
public class NettyRaftNode implements RaftNode {

    private final RaftServer server = new RaftServer();

    private final NodeStateMachine stateMachine = new NodeStateMachine();

    private final WatchDog watchDog = new WatchDog(stateMachine);

    @Override
    public void start() {
        server.start();
        watchDog.startWatch();
    }


    @Override
    public void stop() {
        server.stop();
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
