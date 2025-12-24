package io.terav.vc.net;

import io.terav.vc.net.v0.PacketV0;
import io.terav.vc.net.v0.ProtocolV0;
import io.terav.vc.net.v1.PacketV1;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

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
    
    protected abstract void serialize(ByteBuffer buffer);
    protected abstract int serializedLength();
    public final byte[] serialize() {
        ByteBuffer buffer = ByteBuffer.allocate(serializedLength() + 8);
        buffer.putInt(packet_id);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putShort(proto_ver);
        buffer.put(flags);
        buffer.put(recipient);
        serialize(buffer.slice());
        return buffer.array();
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[packet_id=" + packet_id + ",version=" + Integer.toHexString(this.proto_ver) + "]";
    }
    
    static Packet parse(ByteBuffer buffer) {
        buffer.rewind();
        if (buffer.remaining() < 8) return new InvalidPacket(0, (short) 0, (byte) 0, (byte) 0, InvalidPacket.REASON_LENGTH);
        buffer.order(ByteOrder.BIG_ENDIAN);
        final int packet_id = buffer.getInt();
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        final short version = buffer.getShort();
        final byte flags = buffer.get();
        final byte recipient = buffer.get();
        return switch ((byte) (version >> 8)) {
            case 0 -> PacketV0.parse(packet_id, version, flags, recipient, buffer.slice());
            case 1 -> PacketV1.parse(packet_id, version, flags, recipient, buffer.slice());
            case -1 -> new InvalidPacket(packet_id, version, flags, recipient, InvalidPacket.REASON_VERSION_PROCESS);
            default -> new InvalidPacket(packet_id, version, flags, recipient, InvalidPacket.REASON_VERSION_HIGH);
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
