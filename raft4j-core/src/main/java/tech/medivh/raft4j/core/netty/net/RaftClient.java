package tech.medivh.raft4j.core.netty.net;

import tech.medivh.raft4j.core.NodeInfo;
import tech.medivh.raft4j.core.RaftMessage;
import tech.medivh.raft4j.core.netty.exception.ConnectionException;
import tech.medivh.raft4j.core.netty.exception.MessageException;
import tech.medivh.raft4j.core.netty.processor.NettyMessageProcessor;

import java.util.concurrent.CompletableFuture;

/**
 * @author gongxuanzhangmelt@gmail.com
 **/
public interface RaftClient {

    /**
     * start the client
     **/
    void start();

    /**
     * shutdown the client
     **/
    void shutdown();

    /**
     * register the message processor to the client
     *
     * @param code      the processor support message code
     * @param processor the processor instance
     **/
    void registerMessageProcessor(int code, NettyMessageProcessor processor);


    /**
     * send a request to the server and return a future
     *
     * @param node          the target node
     * @param request       the request message
     * @param timeoutMillis the timeout
     * @return the response future
     **/
    CompletableFuture<ResponseFuture> sendRequest(NodeInfo node, RaftMessage request, long timeoutMillis);

    /**
     * send request to the server and wait for the response.
     * function will block until the response is received or timeout
     *
     * @param node          the target node
     * @param request       the request message
     * @param timeoutMillis the timeout
     * @return the response message
     **/
    RaftMessage sendRequestSync(NodeInfo node, RaftMessage request, long timeoutMillis)
            throws MessageException, InterruptedException, ConnectionException;

    /**
     * send request immediate return.
     * response future will hold the callback instance.
     * the instance will be auto invoke and exact once.
     *
     * @param node     the target node
     * @param request  the request message
     * @param callback the callback instance
     **/
    void sendRequestAsync(NodeInfo node, RaftMessage request, long timeoutMillis, ResponseCallback callback)
            throws MessageException, InterruptedException, ConnectionException;
}
