package tech.medivh.raft4j.bootstrap;

import tech.medivh.raft4j.core.NettyRaftNode;

/**
 * @author gongxuanzhangmelt@gmail.com
 **/
public class StartNode {
    public static void main(String[] args) {
        System.out.println("raft node start ...");
        NettyRaftNode nettyRaftNode = new NettyRaftNode();
        nettyRaftNode.start();
    }
}
