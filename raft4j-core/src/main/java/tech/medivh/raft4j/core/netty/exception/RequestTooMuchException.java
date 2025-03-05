package tech.medivh.raft4j.core.netty.exception;

import tech.medivh.raft4j.core.NodeInfo;

/**
 * @author gongxuanzhangmelt@gmail.com
 **/
public class RequestTooMuchException extends MessageException {

    public RequestTooMuchException(NodeInfo nodeInfo, int limitRequestNumber) {
        this(nodeInfo, limitRequestNumber, null);
    }


    public RequestTooMuchException(NodeInfo nodeInfo, int limitRequestNumber, Throwable cause) {
        super("send request to <" + nodeInfo.addr() + "> failed,limitRequestNumber:" + limitRequestNumber, cause);
    }
}
