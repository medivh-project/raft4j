package tech.medivh.raft4j.core;

import org.junit.jupiter.api.Test;


class NettyRaftNodeTest {

    @Test
    void testRaftNodeStart() throws InterruptedException {
        NettyRaftNode nettyRaftNode = new NettyRaftNode();
        nettyRaftNode.start();
    }


}
