package tech.medivh.raft4j.core.netty.processor;


import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import tech.medivh.raft4j.core.RaftMessage;

/**
 * process netty raft message {@link RaftMessage}
 *
 * @author gongxuanzhangmelt@gmail.com
 **/
public interface NettyMessageProcessor {

    /**
     * like {@link SimpleChannelInboundHandler#channelRead(ChannelHandlerContext, Object)}
     *
     * @param ctx     netty {@link ChannelHandlerContext}
     * @param request request message {@link RaftMessage}
     **/
    void processRequest(ChannelHandlerContext ctx, RaftMessage request);

    /**
     * like {@link SimpleChannelInboundHandler#channelRead(ChannelHandlerContext, Object)}
     *
     * @param ctx      netty {@link ChannelHandlerContext}
     * @param response response message {@link RaftMessage}
     **/
    void processResponse(ChannelHandlerContext ctx, RaftMessage response);
}
