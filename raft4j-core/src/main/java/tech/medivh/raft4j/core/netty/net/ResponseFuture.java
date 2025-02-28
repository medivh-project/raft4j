package tech.medivh.raft4j.core.netty.net;

import tech.medivh.raft4j.core.RaftMessage;

/**
 * @author gongxuanzhangmelt@gmail.com
 **/
public interface ResponseFuture {


    /**
     * response of request id
     **/
    default int getRequestId() {
        return getRequest().getRequestId();
    }

    /**
     * response of request
     **/
    RaftMessage getRequest();

    /**
     * wait for the response until timeout,time out will return null
     **/
    RaftMessage waitResponse(final long timeoutMillis) throws InterruptedException;

    /**
     * like {@link java.util.concurrent.Future}
     * we can set the response to the future
     **/
    void setResponse(RaftMessage response);
}
