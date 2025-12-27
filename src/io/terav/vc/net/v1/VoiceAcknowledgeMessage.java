package io.terav.vc.net.v1;

import java.nio.ByteBuffer;

/**
 * Respond to a VoiceRequestMessage with information about how the request was received.<br>
 * The {@link status} field contains bits set with the {@code STATUS_MASK_*} constants.
 * 
 * <p>Message type {@code 0x64}</p>
 * 
 * @author terav
 */
public class VoiceAcknowledgeMessage extends ResponseMessage {
    /** If this bit is set, the sender is currently connected to another peer */
    public static final byte STATUS_MASK_IN_CALL = 0x10;
    /** If this bit is set, the sender has the peer's identity saved */
    public static final byte STATUS_MASK_KNOWN_CONTACT = 0x20;
    /** If this bit is set, the sender uses a lower minor version than the peer, and some messages may be unreadable */
    public static final byte STATUS_MASK_WARNING_LOWVER = 0x01;
    /** If this bit is set, the sender prefers a smaller databuffer to combat line latency */
    public static final byte STATUS_MASK_WARNING_LATENCY = 0x02;
    /** If this bit is set, the sender prefers data integrity and requires all packets are received at the cost of latency */
    public static final byte STATUS_MASK_WARNING_SEQUENCE = 0x04;
    
    public final byte status;

    public VoiceAcknowledgeMessage(short message_id, short request_id, byte flags) {
        super((byte) 0x64, message_id, request_id);
        this.status = (byte) (flags & 0x37); // strip out unsupported flags
    }

    @Override
    protected void serializeContents(ByteBuffer buf) {
        super.serializeContents(buf);
        buf.put(status);
    }
    @Override
    protected int serializedLength() {
        return super.serializedLength() + 1;
    }
    
    public static VoiceAcknowledgeMessage parse(short message_id, ByteBuffer buf) {
        final short request_id = buf.getShort();
        final byte flags = buf.get();
        return new VoiceAcknowledgeMessage(message_id, request_id, flags);
    }
}
