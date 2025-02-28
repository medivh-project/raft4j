package tech.medivh.raft4j.core.netty.message;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;
import tech.medivh.raft4j.core.RaftMessage;

/**
 *
 * @author gongxuanzhangmelt@gmail.com
 **/
@Slf4j
public class RaftNettyDecoder extends LengthFieldBasedFrameDecoder {


    public RaftNettyDecoder() {
        super(16 * 1024 * 1024 /* aka 16M*/, 0, Integer.BYTES);
    }

    @Override
    public RaftMessage decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf frame = null;
        try {
            frame = (ByteBuf) super.decode(ctx, in);
            return RaftMessage.decode(frame);
        } catch (Exception e) {
            log.error("decode exception, {}", ctx.channel().remoteAddress(), e);
            ctx.channel().close();
        } finally {
            if (frame != null) {
                frame.release();
            }
        }
        return null;
    }
}



