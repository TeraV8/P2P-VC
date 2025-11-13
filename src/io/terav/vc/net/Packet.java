package io.terav.vc.net;

import io.terav.vc.net.v0.PacketV0;
import io.terav.vc.net.v0.ProtocolV0;

public abstract class Packet {
    public final int packet_id;
    public final short proto_ver;
    public final byte flags;
    public final byte recipient;
    
    public static final byte HIGHEST_PROTOVER = 0;
    private static final boolean[] activatedProtocolProcessors = new boolean[HIGHEST_PROTOVER + 1];
    
    protected Packet(int packet_id, short proto_ver, byte flags, byte recipient) {
        this.packet_id = packet_id;
        this.proto_ver = proto_ver;
        this.flags = flags;
        this.recipient = recipient;
    }
    
    protected abstract byte[] data();
    public final byte[] serialize() {
        byte[] data = data();
        byte[] out = new byte[data.length + 8];
        if ((proto_ver >> 8) == 0) {
            out[0] = (byte) (packet_id);
            out[1] = (byte) (packet_id >> 8);
            out[2] = (byte) (packet_id >> 16);
            out[3] = (byte) (packet_id >> 24);
        } else {
            out[0] = (byte) (packet_id >> 24);
            out[1] = (byte) (packet_id >> 16);
            out[2] = (byte) (packet_id >> 8);
            out[3] = (byte) (packet_id);
        }
        out[4] = (byte) (proto_ver);
        out[5] = (byte) (proto_ver >> 8);
        out[6] = flags;
        out[7] = recipient;
        System.arraycopy(data, 0, out, 8, data.length);
        return out;
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[packet_id=" + packet_id + ",version=" + Integer.toHexString(this.proto_ver) + "]";
    }
    
    static Packet parse(byte[] data, int length) {
        if (length < 8) return new InvalidPacket(0, (short) 0, (byte) 0, (byte) 0, InvalidPacket.REASON_LENGTH);
        final int packet_id = (data[0] & 0xFF) | ((data[1] & 0xFF) << 8) | ((data[2] & 0xFF) << 16) | ((data[3] & 0xFF) << 24);
        final short version = (short) ((data[4] & 0xFF) | ((data[5] & 0xFF) << 8));
        return switch (data[5]) {
            case 0 -> PacketV0.parse(packet_id, version, data, length);
            case -1 -> new InvalidPacket(packet_id, version, data[6], data[7], InvalidPacket.REASON_VERSION_PROCESS);
            default -> new InvalidPacket(packet_id, version, data[6], data[7], InvalidPacket.REASON_VERSION_HIGH);
        };
    }
    
    public static synchronized void activateProtocolProcessor(byte version) {
        if (version == -1) version = HIGHEST_PROTOVER;
        if ((version & 0xFF) > HIGHEST_PROTOVER) throw new UnsupportedOperationException("Protocol version " + (version & 0xFF) + " not supported");
        if (activatedProtocolProcessors[version]) return;
        switch (version) {
            case 0 -> ProtocolV0.activateProtocolProcessor();
        }
        activatedProtocolProcessors[version] = true;
    }
}
