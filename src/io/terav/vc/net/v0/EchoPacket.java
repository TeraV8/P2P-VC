package io.terav.vc.net.v0;

import java.nio.ByteBuffer;

public class EchoPacket extends PacketV0 {
    public final long content;

    public EchoPacket(int packet_id, long content) {
        this(packet_id, PacketV0.VERSION_MINOR, content);
    }
    private EchoPacket(int packet_id, byte proto_ver, long content) {
        super(packet_id, proto_ver, (byte) 0);
        this.content = content;
    }

    @Override
    protected void serialize(ByteBuffer buffer) {
        buffer.putLong(content);
    }
    @Override
    protected int serializedLength() {
        return 8;
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[packet_id=" + packet_id + ",version=" + Integer.toHexString(proto_ver) + ",content=" + Long.toHexString(content) + ']';
    }
    
    public static EchoPacket parse(int packet_id, byte proto_ver, ByteBuffer buffer) {
        if (buffer.remaining() != 8) throw new IllegalArgumentException("Data length mismatch");
        return new EchoPacket(packet_id, proto_ver, buffer.getLong());
    }
}
