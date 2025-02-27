package tech.medivh.raft4j.bootstrap;

import tech.medivh.raft4j.core.NettyRaftNode;

/**
 * @author gongxuanzhangmelt@gmail.com
 **/
public class NodeBootStrap {

    public static void main(String[] args) {
        NettyRaftNode nettyRaftNode = new NettyRaftNode();
        nettyRaftNode.start();
    }
}
