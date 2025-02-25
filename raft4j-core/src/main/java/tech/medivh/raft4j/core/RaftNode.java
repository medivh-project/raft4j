package tech.medivh.raft4j.core;

import java.util.List;

/**
 * a node in cluster, have three state{@link NodeState}: candidate leader follower.
 *
 * @author gongxuanzhangmelt@gmail.com
 **/
public interface RaftNode {

    /**
     * start the raft server,and start client
     **/
    void start();


    /**
     * stop the raft server
     **/
    void stop();

    /**
     * return the state of the node
     **/
    NodeState getNodeState();

    /**
     * return the cluster nodes snapshot
     **/
    List<NodeInfo> getClusterNodes();


}
