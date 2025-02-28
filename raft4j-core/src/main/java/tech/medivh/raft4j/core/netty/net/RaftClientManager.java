package tech.medivh.raft4j.core.netty.net;


import lombok.extern.slf4j.Slf4j;
import tech.medivh.raft4j.core.Cluster;

/**
 * @author gongxuanzhangmelt@gmail.com
 **/
@Slf4j
public class RaftClientManager {

    private final Cluster cluster;

    private NettyRaftClient[] clientSessions;

    public RaftClientManager(Cluster cluster) {
        this.cluster = cluster;
    }

    public void connect() {
        clientSessions = new NettyRaftClient[cluster.nodeCount()];
        for (int i = 0; i < cluster.nodeCount(); i++) {
//            clientSessions[i] = new NettyRaftClient(cluster.select(i));
//            clientSessions[i].start();
        }
    }


}
