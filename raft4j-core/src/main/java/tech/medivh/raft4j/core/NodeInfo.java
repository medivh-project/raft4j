package tech.medivh.raft4j.core;

import lombok.Data;

/**
 * raft node info snapshot.
 *
 * @author gongxuanzhangmelt@gmail.com
 **/
@Data
public class NodeInfo {

    private final String host;
    private final int port;

    public NodeInfo(String host, int port) {
        this.host = host;
        this.port = port;
    }

}
