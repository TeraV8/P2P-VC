package io.terav.vc.net.v1;

import java.nio.ByteBuffer;

public final class EchoPacket extends PacketV1 {
    public final long data;

    public EchoPacket(int packet_id, long data) {
        this(packet_id, PacketV1.VERSION_MINOR, data);
    }
    public EchoPacket(int packet_id, byte proto_ver, long data) {
        super(packet_id, proto_ver, (byte) 0, (byte) 0);
        this.data = data;
    }
    
    @Override
    protected void serialize(ByteBuffer buffer) {
        buffer.putLong(data);
    }
    @Override
    protected int serializedLength() {
        return 8;
    }
    
    public static EchoPacket parse(int packet_id, ByteBuffer buffer) {
        final long data = buffer.getLong();
        return new EchoPacket(packet_id, buffer.get(5), data);
    }
}
