package io.terav.vc.net.v0;

import io.terav.vc.net.InvalidPacket;
import io.terav.vc.net.Packet;

public abstract class PacketV0 extends Packet {
    public static byte VERSION_MINOR = 0;
    
    protected PacketV0(int packet_id, byte proto_ver, byte flags) {
        this(packet_id, proto_ver, flags, (byte) 0);
    }
    protected PacketV0(int packet_id, byte proto_ver, byte flags, byte recipient) {
        super(packet_id, (short) (proto_ver & 0xFF), flags, recipient);
    }
    
    public static Packet parse(int packet_id, short version, byte[] data, int length) {
        if (((version >>> 8) & 0xFF) != 0) throw new IllegalArgumentException("Invalid packet version");
        return switch (data[6] & 0x3) {
            case 0 -> EchoPacket.parse(packet_id, (byte) version, data, length);
            case 1 -> ProtoPacket.parse(packet_id, (byte) version, data, length);
            case 2 -> DataPacket.parse(packet_id, (byte) version, data, length);
            case 3 -> new InvalidPacket(packet_id, version, data[6], data[7], InvalidPacket.REASON_MALFORMED_HEADER);
            default -> null; // impossible
        };
    }
}
