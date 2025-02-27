package tech.medivh.raft4j.core.netty.message;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * netty raft message
 * message protocol：
 * <ol>
 *    <li>length (4 bytes)</li>
 *    <li>version (1 byte)</li>
 *    <li>type (2 bytes)</li>
 *    <li>body (length - 4 - 1 - 2 -4)</li>
 *    <li>checksum (4 bytes)</li>
 *  </ol>
 * checksum rule:
 * <p>
 * ~(length + body[first] * body[last] )
 * </p>
 *
 * @author gongxuanzhangmelt@gmail.com
 **/
public class RaftMessage {
    
    private static final AtomicInteger REQUEST_SEQ = new AtomicInteger(0);

    private final int typeCode;

    private final int version;

    private byte[] body;


    public RaftMessage(int typeCode, int version) {
        this.typeCode = typeCode;
        this.version = version;
    }


    public static RaftMessage decode(ByteBuf byteBuffer) throws DecoderException {
        int length = byteBuffer.readableBytes();
        byte version = byteBuffer.readByte();
        short type = byteBuffer.readShort();
        int bodyLength = length - 4 - 1 - 2 - 4;
        if (bodyLength < 0) {
            throw new DecoderException("body length is negative");
        }
        byte[] body = new byte[bodyLength];
        byteBuffer.readBytes(body);
        int checksum = byteBuffer.readInt();
        if (checksum != (~(length + body[0] * body[body.length - 1]))) {
            throw new DecoderException("checksum error");
        }
        RaftMessage message = new RaftMessage(type, version);
        message.body = body;
        return message;
    }


    public ByteBuffer encode() {
        int length = 4 + 1 + 2 + (body == null ? 0 : body.length) + 4;
        byte first = body == null ? 0 : body[0];
        byte last = body == null ? 0 : body[body.length - 1];
        int checksum = ~(length + first * last);
        ByteBuffer byteBuffer = ByteBuffer.allocate(length);
        byteBuffer.putInt(length)
                .put((byte) version)
                .putShort((short) typeCode);
        if (body != null) {
            byteBuffer.put(body);
        }
        byteBuffer.putInt(checksum);
        byteBuffer.flip();
        return byteBuffer;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public byte[] getBody() {
        return body;
    }

    public RaftMessageType getMessageType() {
        return RaftMessageType.values()[typeCode];
    }


}
