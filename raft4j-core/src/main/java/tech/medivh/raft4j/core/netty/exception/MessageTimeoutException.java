package tech.medivh.raft4j.core.netty.exception;

import tech.medivh.raft4j.core.NodeInfo;

/**
 * @author gongxuanzhangmelt@gmail.com
 **/
public class MessageTimeoutException extends MessageException {

    public MessageTimeoutException(NodeInfo nodeInfo, long timeoutMillis) {
        this(nodeInfo, timeoutMillis, null);
    }


    public MessageTimeoutException(NodeInfo nodeInfo, long timeoutMillis, Throwable cause) {
        super("send request to <" + nodeInfo.addr() + "> failed,timeoutMillis:" + timeoutMillis, cause);
    }
}
