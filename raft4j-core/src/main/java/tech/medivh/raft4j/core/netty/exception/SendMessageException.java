package tech.medivh.raft4j.core.netty.exception;


import tech.medivh.raft4j.core.NodeInfo;

/**
 * @author gongxuanzhangmelt@gmail.com
 **/
public class SendMessageException extends MessageException {


    public SendMessageException(NodeInfo nodeInfo) {
        this(nodeInfo, null);
    }

    public SendMessageException(NodeInfo nodeInfo, Throwable cause) {
        super("send request to <" + nodeInfo.addr() + "> failed", cause);
    }
}
