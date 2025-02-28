package tech.medivh.raft4j.core.netty.net;

import tech.medivh.raft4j.core.RaftMessage;

/**
 * @author gongxuanzhangmelt@gmail.com
 **/
public class LocalResponseFuture implements ResponseFuture {

    private final RaftMessage request;

    private final RaftMessage localResponse;

    public LocalResponseFuture(RaftMessage request, RaftMessage localResponse) {
        this.request = request;
        this.localResponse = localResponse;
    }

    @Override
    public RaftMessage getRequest() {
        return request;
    }

    @Override
    public RaftMessage waitResponse(long timeoutMillis) {
        return localResponse;
    }

    @Override
    public void setResponse(RaftMessage response) {
        throw new UnsupportedOperationException("local response future can not set response");
    }
}
