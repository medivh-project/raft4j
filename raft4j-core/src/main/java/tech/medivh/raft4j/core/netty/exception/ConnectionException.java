package tech.medivh.raft4j.core.netty.exception;


import tech.medivh.raft4j.core.NodeInfo;

/**
 * @author gongxuanzhangmelt@gmail.com
 **/
public class ConnectionException extends Exception {
    public ConnectionException(NodeInfo nodeInfo) {
        super("connect to node " + nodeInfo + " failed");
    }
}
