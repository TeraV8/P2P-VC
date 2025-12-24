package io.terav.vc.net.v0;

import java.nio.ByteBuffer;

public final class ProtoDowngradeMessage extends Message {
    public final short version;

    public ProtoDowngradeMessage(short message_id) {
        this(message_id, (short)(PacketV0.VERSION_MINOR & 0xFF));
    }
    private ProtoDowngradeMessage(short message_id, short version) {
        super((byte) 0xF0, message_id);
        this.version = version;
    }

    @Override
    protected void serializeMessage(ByteBuffer buffer) {
        buffer.putShort(version);
    }
    @Override
    protected int serializedLength() {
        return 2;
    }
    
    public static ProtoDowngradeMessage parse(short message_id, ByteBuffer buffer) {
        if (buffer.remaining() != 2) throw new IllegalArgumentException("Message length invalid");
        return new ProtoDowngradeMessage(message_id, buffer.getShort());
    }
}
