package io.terav.vc.net.v1;

import java.nio.ByteBuffer;

/**
 * Notify the peer to use protocol v1 and the associated minor version.
 * 
 * @author terav
 */
public class ProtoDowngradeMessage extends Message {

    public ProtoDowngradeMessage(short message_id) {
        super((byte) 0x7E, message_id);
    }

    @Override
    public int getPriority() {
        return 7;
    }
    @Override
    protected void serializeContents(ByteBuffer buf) {}
    @Override
    protected int serializedLength() {
        return 0;
    }
}
