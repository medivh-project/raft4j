package tech.medivh.raft4j.core.netty.exception;


/**
 * @author gongxuanzhangmelt@gmail.com
 **/
public class MessageException extends RuntimeException {

    public MessageException(String message) {
        super(message);
    }

    public MessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
