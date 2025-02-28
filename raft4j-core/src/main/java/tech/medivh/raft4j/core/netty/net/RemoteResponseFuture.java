package tech.medivh.raft4j.core.netty.net;

import lombok.extern.slf4j.Slf4j;
import tech.medivh.raft4j.core.RaftMessage;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author gongxuanzhangmelt@gmail.com
 **/
@Slf4j
public class RemoteResponseFuture implements ResponseFuture {

    private final RaftMessage request;

    private final CountDownLatch countDownLatch = new CountDownLatch(1);

    private volatile RaftMessage response;

    public RemoteResponseFuture(RaftMessage request) {
        this.request = request;
    }


    @Override
    public RaftMessage getRequest() {
        return null;
    }

    @Override
    public RaftMessage waitResponse(long timeoutMillis) throws InterruptedException {
        boolean get = countDownLatch.await(timeoutMillis, TimeUnit.MILLISECONDS);
        if (!get) {
            log.warn("wait response timeout,requestId:{}, But there is still a chance of a result ",
                    request.getRequestId());
        }
        return response;
    }

    @Override
    public void setResponse(RaftMessage response) {
        this.response = response;
        countDownLatch.countDown();
    }
}
