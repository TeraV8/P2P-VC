package io.terav.vc.net.v1;

import java.nio.ByteBuffer;

/**
 * Placeholder class for all message types whose only data is a 2-byte request ID.
 * @author terav
 */
public class ResponseMessage extends Message {
    public static final byte VOICE_ACCEPT = 0x66;
    public static final byte VOICE_REJECT = 0x67;
    
    public final short request_id;

    public ResponseMessage(byte type, short message_id, short request_id) {
        super(type, message_id);
        this.request_id = request_id;
    }

    @Override
    protected void serializeContents(ByteBuffer buf) {
        buf.putShort(request_id);
    }
    @Override
    protected int serializedLength() {
        return 2;
    }
    
    public static ResponseMessage parse(byte type, short message_id, ByteBuffer buf) {
        final short request_id = buf.getShort();
        return new ResponseMessage(type, message_id, request_id);
    }
}
