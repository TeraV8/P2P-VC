package io.terav.vc.net.v0;

import java.nio.ByteBuffer;

public class IPRequestMessage extends Message {

    public IPRequestMessage(short message_id) {
        super((byte) 0xC0, message_id);
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
