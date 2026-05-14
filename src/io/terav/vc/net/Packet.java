package io.terav.vc.net;

import io.terav.vc.net.v0.PacketV0;
import io.terav.vc.net.v1.PacketV1;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public abstract class Packet {
    public final int packet_id;
    public final short proto_ver;
    public final byte flags;
    public final byte recipient;
    
    
    protected Packet(int packet_id, short proto_ver, byte flags, byte recipient) {
        this.packet_id = packet_id;
        this.proto_ver = proto_ver;
        this.flags = flags;
        this.recipient = recipient;
    }
    
    /**
     * Convert the information in this packet to binary data.
     * Only write the contents of the packet; the header is handled by a wrapper method.
     * @param buffer The buffer in which to write data
     */
    protected abstract void serialize(ByteBuffer buffer);
    /**
     * Returns the length of the inner serialized data. Does not include the header.
     * @return The length of the serialized data
     */
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
        try {
            return switch ((byte) (version >> 8)) {
                case 0 -> PacketV0.parse(packet_id, version, flags, recipient, buffer.slice());
                case 1 -> PacketV1.parse(packet_id, version, flags, recipient, buffer.slice());
                case -1 -> new InvalidPacket(packet_id, version, flags, recipient, InvalidPacket.REASON_VERSION_PROCESS);
                default -> new InvalidPacket(packet_id, version, flags, recipient, InvalidPacket.REASON_VERSION_HIGH);
            };
        } catch (Exception e) {
            return new InvalidPacket(packet_id, version, flags, recipient, InvalidPacket.REASON_INVALID_DATA);
        }
    }
}
