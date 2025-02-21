package tech.medivh.raft4j.core.netty.message;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;

import java.util.List;

/**
 * raft message codec. <br/>
 * message protocol：
 * <ol>
 *    <li>length (4 bytes)</li>
 *    <li>magic 9af5 cafe(raft cafe) (4 bytes)</li>
 *    <li>version (1 byte)</li>
 *    <li>type (2 bytes)</li>
 *    <li>body (length - 4 - 4 - 1 - 2 -4)</li>
 *    <li>checksum (4 bytes)</li>
 *  </ol>
 * checksum rule:
 * <p>
 * !(length + body[first] * body[last] )
 * </p>
 *
 * @author gongxuanzhangmelt@gmail.com
 **/
public class RaftMessageCodec extends ByteToMessageCodec<RaftMessage<?>> {


    @Override
    public void encode(ChannelHandlerContext ctx, RaftMessage<?> msg, ByteBuf out) throws Exception {

    }

    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    }
}
