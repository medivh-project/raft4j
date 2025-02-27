package tech.medivh.raft4j.core.netty.message;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;

import java.util.List;

/**
 * @author gongxuanzhangmelt@gmail.com
 **/
@ChannelHandler.Sharable
public class RaftMessageCodec extends ByteToMessageCodec<RaftMessage> {

    private final RaftNettyDecoder decoder = new RaftNettyDecoder();

    private final RaftNettyEncoder encoder = new RaftNettyEncoder();


    @Override
    public void encode(ChannelHandlerContext ctx, RaftMessage msg, ByteBuf out) throws Exception {
        encoder.encode(ctx, msg, out);
    }

    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        RaftMessage raftMessage = decoder.decode(ctx, in);
        out.add(raftMessage);
    }

}
