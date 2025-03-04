package tech.medivh.raft4j.core.netty.net;

import lombok.extern.slf4j.Slf4j;
import tech.medivh.raft4j.core.NodeInfo;
import tech.medivh.raft4j.core.RaftMessage;
import tech.medivh.raft4j.core.netty.exception.SendMessageException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author gongxuanzhangmelt@gmail.com
 **/
@Slf4j
public class RemoteResponseFuture implements ResponseFuture {

    private final RaftMessage request;

    private final CountDownLatch countDownLatch = new CountDownLatch(1);

    private final long startTime = System.currentTimeMillis();

    private final long timeoutMillis;

    private final AtomicBoolean exactlyOnce = new AtomicBoolean(false);

    private final ResponseCallback callback;

    private final NodeInfo nodeInfo;

    private volatile RaftMessage response;

    private volatile boolean sendRequestSuccess = false;

    private volatile Throwable cause;

    public RemoteResponseFuture(RaftMessage request, NodeInfo nodeInfo, long timeoutMillis, ResponseCallback callback) {
        this.request = request;
        this.callback = callback;
        this.timeoutMillis = timeoutMillis;
        this.nodeInfo = nodeInfo;
    }


    @Override
    public RaftMessage getRequest() {
        return request;
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
    public RaftMessage getResponse() {
        return response;
    }

    @Override
    public void setResponse(RaftMessage response) {
        this.response = response;
        countDownLatch.countDown();
    }

    @Override
    public void setSendRequestSuccess() {
        this.sendRequestSuccess = true;
    }

    @Override
    public boolean sendRequestSuccess() {
        return sendRequestSuccess;
    }

    @Override
    public void executeCallback() {
        if (this.callback == null || !exactlyOnce.compareAndSet(false, true)) {
            return;
        }
        if (this.response == null) {
            this.callback.onResponse(response);
            return;
        }
        if (!this.sendRequestSuccess) {
            this.callback.onException(new SendMessageException(nodeInfo, get));
        }
    }

    @Override
    public void setCause(Throwable cause) {
        this.cause = cause;
    }

    @Override
    public Throwable getCause() {
        return this.cause;
    }
}
