package tech.medivh.raft4j.core.netty.net;


import lombok.extern.slf4j.Slf4j;
import tech.medivh.raft4j.core.Cluster;

/**
 * @author gongxuanzhangmelt@gmail.com
 **/
@Slf4j
public class RaftClientManager {

    private final Cluster cluster;


    public RaftClientManager(Cluster cluster) {
        this.cluster = cluster;
    }

}
