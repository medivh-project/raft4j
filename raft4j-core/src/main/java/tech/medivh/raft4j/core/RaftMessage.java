package tech.medivh.raft4j.core;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * netty raft message
 * message protocol：
 * <ol>
 *    <li>length (4 bytes)</li>
 *    <li>version (2 byte)</li>
 *    <li>type (4 bytes)</li>
 *    <li> request id </>
 *    <li>body (length - 4 - 4 - 2 -4 -4)</li>
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

    private final int code;

    private final int version;

    private int requestId = REQUEST_SEQ.getAndIncrement();

    private byte[] body;


    public RaftMessage(int code) {
        this(code, Raft4J.CURRENT_VERSION);
    }

    public RaftMessage(int code, int version) {
        this.code = code;
        this.version = version;
    }

    public static RaftMessage createResponse(int code, byte[] body) {
        RaftMessage message = new RaftMessage(code);
        message.body = body;
        return message;
    }

    public static RaftMessage createResponse(int code, String body) {
        RaftMessage message = new RaftMessage(code);
        message.body = body.getBytes(StandardCharsets.UTF_8);
        return message;
    }


    public static RaftMessage decode(ByteBuf byteBuffer) throws DecoderException {
        int length = byteBuffer.readableBytes();
        int version = byteBuffer.readUnsignedShort();
        int type = byteBuffer.readInt();
        int requestId = byteBuffer.readInt();
        int bodyLength = length - 4 - 4 - 2 - 4 - 4;
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
        message.requestId = requestId;
        return message;
    }


    public ByteBuffer encode() {
        int length = 4 + 1 + 2 + (body == null ? 0 : body.length) + 4;
        byte first = body == null ? 0 : body[0];
        byte last = body == null ? 0 : body[body.length - 1];
        int checksum = ~(length + first * last);
        ByteBuffer byteBuffer = ByteBuffer.allocate(length);
        byteBuffer.putInt(length)
                .putShort((short) version)
                .putInt(code)
                .putInt(requestId);
        if (body != null) {
            byteBuffer.put(body);
        }
        byteBuffer.putInt(checksum);
        byteBuffer.flip();
        return byteBuffer;
    }

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public byte[] getBody() {
        return body;
    }

    public int getCode() {
        return code;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "RaftMessage{" +
                "code=" + code +
                ", version=" + version +
                ", requestId=" + requestId +
                '}';
    }
}
