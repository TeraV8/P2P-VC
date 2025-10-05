package io.terav.vc.net;

/**
 * A {@link Packet} that, for all intents and purposes, could not be parsed, due to a missing protocol processor or invalid packet format.
 * Receipt of an {@code InvalidPacket} consitutes a communication-related {@code Exception}.
 * @author vharr
 */
public final class InvalidPacket extends Packet {
    /** The packet was invalid due to an undisclosed reason. */
    public static final int REASON_NONE = 0;
    /** The packet was invalid due to the data being too short. */
    public static final int REASON_LENGTH = 1;
    /** The packet was invalid because it uses a higher version not supported. */
    public static final int REASON_VERSION_HIGH = 16;
    /** The packet was invalid because it uses the IPC packet version but was received remotely. */
    public static final int REASON_VERSION_PROCESS = 17;
    /** The packet was invalid because it uses a lower version which is no longer supported. */
    public static final int REASON_VERSION_LOW = 18;
    /** The packet was invalid due to a malformed packet header. */
    public static final int REASON_MALFORMED_HEADER = 32;
    
    public final int reason;
    
    public InvalidPacket(int packet_id, short version, byte flags, byte recipient, int reason) {
        super(packet_id, version, flags, recipient);
        this.reason = reason;
    }
    
    @Override
    protected byte[] data() {
        throw new UnsupportedOperationException("Invalid packets cannot be serialized");
    }
}
