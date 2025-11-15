package io.terav.vc.net.v0;

import java.nio.ByteBuffer;

public final class InvalidMessage extends Message {
    
    InvalidMessage(byte type, short message_id) {
        super(type, message_id);
    }

    @Override
    protected void serializeMessage(ByteBuffer buffer) {
        throw new UnsupportedOperationException("Invalid messages cannot be serialized");
    }
    @Override
    protected int serializedLength() {
        throw new UnsupportedOperationException("Invalid messages cannot be serialized");
    }
}
