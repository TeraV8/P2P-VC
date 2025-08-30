package io.terav.vc.net.v0;

import io.terav.vc.net.Packet;

public abstract class PacketV0 extends Packet {
    public static byte VERSION_MINOR = 0;
    
    protected PacketV0(int packet_id, byte proto_ver, byte flags) {
        super(packet_id, (short) (proto_ver & 0xFF), flags, (byte) 0);
    }
    
    public static PacketV0 parse(byte[] data, int length) {
        if (length > data.length) throw new ArrayIndexOutOfBoundsException("Length parameter cannot exceed length of the array");
        if (length < 8) throw new IllegalArgumentException("Data too short");
        final int packet_id = (data[0] & 0xFF) | ((data[1] & 0xFF) << 8) | ((data[2] & 0xFF) << 16) | ((data[3] & 0xFF) << 24);
        final short proto_ver = (short) ((data[4] & 0xFF) | ((data[5] & 0xFF) << 8));
        if (((proto_ver >>> 8) & 0xFF) != 0) throw new IllegalArgumentException("Invalid packet version");
        return switch (data[6] & 0x3) {
            case 0 -> EchoPacket.parse(packet_id, (byte) proto_ver, data, length);
            case 1 -> ProtoPacket.parse(packet_id, (byte) proto_ver, data, length);
            case 2 -> DataPacket.parse(packet_id, (byte) proto_ver, data, length);
            case 3 -> throw new IllegalArgumentException("Invalid packet flags");
            default -> null; // impossible
        };
    }
}
