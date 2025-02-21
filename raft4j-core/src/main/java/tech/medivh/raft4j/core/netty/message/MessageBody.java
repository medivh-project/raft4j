package tech.medivh.raft4j.core.netty.message;

/**
 * body in message.
 *
 * @author gongxuanzhangmelt@gmail.com
 **/
public interface MessageBody {

    /**
     * @return body in bytes
     **/
    byte[] toBytes();
}
