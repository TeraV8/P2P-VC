package io.terav.vc.net.v0;

import java.util.Arrays;

public abstract class Message {
    public final byte type;
    public final short message_id;
    
    protected Message(byte type, short message_id) {
        this.type = type;
        this.message_id = message_id;
    }
    
    protected abstract byte[] data();
    public final byte[] serialize() {
        byte[] subdata = data();
        if (subdata.length > 255) throw new IllegalStateException("Data too long");
        byte[] data = new byte[subdata.length + 4];
        data[0] = (byte) subdata.length;
        data[1] = type;
        data[2] = (byte) message_id;
        data[3] = (byte)(message_id >> 8);
        System.arraycopy(subdata, 0, data, 4, subdata.length);
        return data;
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[message_id=" + message_id + ']';
    }
    
    public static Message parse(byte[] data, int offset) {
        try {
            int length = data[offset] & 0xFF;
            byte type = data[offset + 1];
            short message_id = (short) ((data[offset + 2] & 0xFF) | ((data[offset + 3] & 0xFF) << 8));
            return switch (type) {
                case (byte) 0x30 -> VCRequestMessage.parse(message_id, data, offset + 4, length);
                case (byte) 0x31 -> VCRequestAcknowledgeMessage.parse(message_id, data, offset + 4, length);
                case (byte) 0x32 -> VCAcceptMessage.parse(message_id, data, offset + 4, length);
                case (byte) 0x33 -> VCRejectMessage.parse(message_id, data, offset + 4, length);
                case (byte) 0x34 -> VCDisconnectMessage.parse(message_id, data, offset + 4, length);
                case (byte) 0xC0 -> IPRequestMessage.parse(message_id, data, offset + 4, length);
                case (byte) 0xC1 -> IPResponseMessage.parse(message_id, data, offset + 4, length);
                case (byte) 0xF0 -> ProtoDowngradeMessage.parse(message_id, data, offset + 4, length);
                default -> throw new IllegalArgumentException("Unrecognized message type " + type);
            };
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Data too short", e);
        }
    }
}
