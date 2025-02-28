package tech.medivh.raft4j.core.netty.exception;

/**
 * @author gongxuanzhangmelt@gmail.com
 **/
public class MessageTimeoutException extends Exception {

    public MessageTimeoutException(String addr) {
        this(addr, null);
    }

    public MessageTimeoutException(String addr, Throwable cause) {
        super("send request to <" + addr + "> failed", cause);
    }
}
