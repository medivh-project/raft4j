package tech.medivh.raft4j.core.netty.message;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import tech.medivh.raft4j.core.RaftMessage;

/**
 * @author gongxuanzhangmelt@gmail.com
 **/
public class RaftNettyEncoder extends MessageToByteEncoder<RaftMessage> {
    
    
    
    @Override
    public void encode(ChannelHandlerContext ctx, RaftMessage msg, ByteBuf out) throws Exception {
        
    }
}
