package tech.medivh.raft4j.core.netty.net;

import tech.medivh.raft4j.core.RaftMessage;
import tech.medivh.raft4j.core.RaftNode;
import tech.medivh.raft4j.core.netty.exception.MessageTimeoutException;
import tech.medivh.raft4j.core.netty.processor.NettyMessageProcessor;

/**
 * @author gongxuanzhangmelt@gmail.com
 **/
public interface RaftClient {

    /**
     * start the client
     **/
    void start();

    /**
     * close the connection
     **/
    void stop();

    /**
     * register the message processor to the client
     * @param code the processor support message code
     * @param processor the processor instance
     **/
    void registerMessageProcessor(int code, NettyMessageProcessor processor);

    /**
     * send request to the server and wait for the response.
     * function will block until the response is received or timeout
     *
     * @param node          the target node
     * @param request       the request message
     * @param timeoutMillis the timeout
     **/
    ResponseFuture sendRequestSync(RaftNode node, RaftMessage request, long timeoutMillis) throws MessageTimeoutException, InterruptedException;

    /**
     * send request immediate return.
     * response future will hold the callback instance.
     * the instance will be auto invoke and exact once.
     *
     * @param node     the target node
     * @param request  the request message
     * @param callback the callback instance
     **/
    ResponseFuture sendRequestAsync(RaftNode node, RaftMessage request, ResponseCallback callback);
}
