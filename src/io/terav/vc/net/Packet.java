package io.terav.vc.net;

import io.terav.vc.NetworkManager;
import io.terav.vc.net.v0.PacketV0;
import io.terav.vc.net.v0.ProtocolV0;
import java.net.InetSocketAddress;

public abstract class Packet {
    public final int packet_id;
    public final short proto_ver;
    public final byte flags;
    public final byte recipient;
    InetSocketAddress remote;
    
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
        out[0] = (byte) (packet_id);
        out[1] = (byte) (packet_id >> 8);
        out[2] = (byte) (packet_id >> 16);
        out[3] = (byte) (packet_id >> 24);
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
    
    public static Packet parse(byte[] data, int length) {
        if (length < 6) throw new IllegalArgumentException("Data too short");
        return switch (data[5]) {
            case 0 -> PacketV0.parse(data, length);
            default -> throw new UnsupportedOperationException("Packet version " + data[5] + '.' + data[4] + " not supported");
        };
    }
    
    public static synchronized void activateProtocolProcessor(byte version, NetworkManager man) {
        if (version == -1) version = HIGHEST_PROTOVER;
        if ((version & 0xFF) > HIGHEST_PROTOVER) throw new UnsupportedOperationException("Protocol version " + (version & 0xFF) + " not supported");
        if (activatedProtocolProcessors[version]) return;
        switch (version) {
            case 0 -> ProtocolV0.activateProtocolProcessor(man);
        }
        activatedProtocolProcessors[version] = true;
    }
}
