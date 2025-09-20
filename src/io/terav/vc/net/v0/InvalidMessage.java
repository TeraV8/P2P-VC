package io.terav.vc.net.v0;

public final class InvalidMessage extends Message {
    
    InvalidMessage(byte type, short message_id) {
        super(type, message_id);
    }

    @Override
    protected byte[] data() {
        throw new UnsupportedOperationException("Invalid messages cannot be serialized");
    }
}
