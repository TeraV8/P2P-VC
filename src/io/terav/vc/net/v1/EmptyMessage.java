package io.terav.vc.net.v1;

import java.nio.ByteBuffer;

/**
 * Placeholder class for all message types that have no data.
 * @author terav
 */
public final class EmptyMessage extends Message {
    public static final byte PROTO_REQUEST_IDENTIFY = 0x75;
    public static final byte PROTO_DOWNGRADE = 0x7E;

    public EmptyMessage(byte type, short message_id) {
        super(type, message_id);
    }
    @Override
    protected void serializeContents(ByteBuffer buf) {}
    @Override
    protected int serializedLength() {
        return 0;
    }
}
