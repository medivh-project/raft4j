package tech.medivh.raft4j.core.netty.message;

/**
 * 
 * netty raft message
 * 
 * @param <B> body
 * @author gongxuanzhangmelt@gmail.com
 **/
public interface RaftMessage<B> {
    
    
    /**
     * @return message record  
     **/
    B getBody();
    
    /**
     * @return message type
     **/
    RaftMessageType getMessageType();
    
    
}
