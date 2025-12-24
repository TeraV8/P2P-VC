package io.terav.vc.net.v0;

import java.nio.ByteBuffer;

public final class VCDisconnectMessage extends Message {
    public VCDisconnectMessage(short message_id) {
        super((byte) 0x34, message_id);
    }
    
    @Override
    protected void serializeMessage(ByteBuffer buffer) {
        // nothing to serialize
    }
    @Override
    protected int serializedLength() {
        return 0;
    }
}
