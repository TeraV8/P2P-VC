package io.terav.vc.net.v0;

import java.nio.ByteBuffer;

public final class VCRequestAcknowledgeMessage extends Message {
    public final short request_id;

    public VCRequestAcknowledgeMessage(short message_id, short request_id) {
        super((byte) 0x31, message_id);
        this.request_id = request_id;
    }

    @Override
    protected void serializeMessage(ByteBuffer buffer) {
        buffer.putShort(request_id);
    }
    @Override
    protected int serializedLength() {
        return 2;
    }
    
    public static VCRequestAcknowledgeMessage parse(short message_id, ByteBuffer buffer) {
        if (buffer.remaining() != 2) throw new IllegalArgumentException("Message length invalid");
        return new VCRequestAcknowledgeMessage(message_id, buffer.getShort());
    }
}
