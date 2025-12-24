package io.terav.vc.net.v1;

import java.nio.ByteBuffer;

public final class InvalidMessage extends Message {
    
    InvalidMessage(short message_id, byte type) {
        super(type, message_id);
    }

    @Override
    protected void serializeContents(ByteBuffer buffer) {
        throw new UnsupportedOperationException("Invalid messages cannot be serialized");
    }
    @Override
    protected int serializedLength() {
        throw new UnsupportedOperationException("Invalid messages cannot be serialized");
    }
}
