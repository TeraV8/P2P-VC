package io.terav.vc.net.v0;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public abstract sealed class Message permits InvalidMessage, IPRequestMessage,
        IPResponseMessage, ProtoDowngradeMessage, ReceiptMessage, VCAcceptMessage,
        VCDisconnectMessage, VCRejectMessage, VCRequestAcknowledgeMessage, VCRequestMessage {
    public final byte type;
    public final short message_id;
    
    protected Message(byte type, short message_id) {
        this.type = type;
        this.message_id = message_id;
    }
    
    protected abstract void serializeMessage(ByteBuffer buffer);
    protected abstract int serializedLength();
    public final void serialize(ByteBuffer buffer) {
        int length = serializedLength();
        if (length > 255) throw new IllegalStateException("Data too long");
        buffer.put((byte) length);
        buffer.put(type);
        buffer.putShort(message_id);
        int position = buffer.position();
        serializeMessage(buffer.slice(position, length).order(ByteOrder.LITTLE_ENDIAN));
        buffer.position(position + length);
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[message_id=" + message_id + ']';
    }
    
    public static Message parse(ByteBuffer buffer) {
        try {
            int length = buffer.get() & 0xFF;
            byte type = buffer.get();
            short message_id = buffer.getShort();
            int initialLimit = buffer.limit();
            buffer.limit(buffer.position() + length);
            Message m = switch (type) {
                case (byte) 0x30 -> VCRequestMessage.parse(message_id, buffer);
                case (byte) 0x31 -> VCRequestAcknowledgeMessage.parse(message_id, buffer);
                case (byte) 0x32 -> VCAcceptMessage.parse(message_id, buffer);
                case (byte) 0x33 -> VCRejectMessage.parse(message_id, buffer);
                case (byte) 0x34 -> new VCDisconnectMessage(message_id); // functionally identical
                case (byte) 0xC0 -> new IPRequestMessage(message_id); // functionally identical
                case (byte) 0xC1 -> IPResponseMessage.parse(message_id, buffer);
                case (byte) 0xF0 -> ProtoDowngradeMessage.parse(message_id, buffer);
                case (byte) 0xF2 -> ReceiptMessage.parse(message_id, buffer);
                default -> {
                    yield new InvalidMessage(type, message_id);
                }
            };
            buffer.position(buffer.limit());
            buffer.limit(initialLimit);
            return m;
        } catch (BufferUnderflowException e) {
            throw new IllegalArgumentException("Data too short", e);
        }
    }
}
