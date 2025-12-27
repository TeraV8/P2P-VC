package io.terav.vc.net.v1;

import io.terav.vc.net.Packet;
import java.nio.ByteBuffer;

public abstract class PacketV1 extends Packet {
    public static final byte VERSION_MINOR = 0;
    
    protected PacketV1(int packet_id, byte proto_ver, byte flags, byte recipient) {
        super(packet_id, (short) ((proto_ver & 0xFF) | 0x100), flags, recipient);
    }
    
    public static Packet parse(int packet_id, short version, byte flags, byte recipient, ByteBuffer buffer) {
        if (((version >> 8) & 0xFF) != 1) throw new IllegalArgumentException("Invalid packet version");
        return switch (flags & 0x3) {
            case 0 -> EchoPacket.parse(packet_id, buffer);
            case 1 -> DataPacket.parse(packet_id, recipient, buffer);
            case 2, 3 -> ProtoPacket.parse(packet_id, flags, recipient, buffer);
            default -> null;
        };
    }
}
