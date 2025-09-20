package io.terav.vc.net;

/**
 * A {@link Packet} that, for all intents and purposes, could not be parsed, due to a missing protocol processor or invalid packet format.
 * Receipt of an {@code InvalidPacket} consitutes a communication-related {@code Exception}.
 * @author vharr
 */
public final class InvalidPacket extends Packet {
    
    private InvalidPacket(int packet_id, short version, byte flags, byte recipient) {
        super(packet_id, version, flags, recipient);
    }
    
    @Override
    protected byte[] data() {
        throw new UnsupportedOperationException("Invalid packets cannot be serialized");
    }
    static InvalidPacket parse(byte[] data, int length) {
        if (length < 8) throw new IllegalArgumentException("Data too short");
        return new InvalidPacket(
                ((data[0] & 0xFF) | ((data[1] & 0xFF) << 8) | ((data[2] & 0xFF) << 16) | ((data[3] & 0xFF) << 24)),
                (short) ((data[4] & 0xFF) | ((data[5] & 0xFF) << 8)),
                data[6],
                data[7]
        );
    }
}
