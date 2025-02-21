package tech.medivh.raft4j.core.netty.message;

import java.nio.ByteBuffer;

/**
 * A message body should correspond to a decoder.
 *
 * @author gongxuanzhangmelt@gmail.com
 **/
public interface BodyDecoder<B extends MessageBody> {

    /**
     * decode the bytes to body
     **/
    B decode(ByteBuffer bytes);
}
