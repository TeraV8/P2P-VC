package io.terav.vc.net.v0;

public class VCRequestAcknowledgeMessage extends Message {
    public final short request_id;

    public VCRequestAcknowledgeMessage(short message_id, short request_id) {
        super((byte) 0x31, message_id);
        this.request_id = request_id;
    }

    @Override
    protected byte[] data() {
        byte[] data = new byte[2];
        data[0] = (byte) request_id;
        data[1] = (byte)(request_id >> 8);
        return data;
    }
    
    public static VCRequestAcknowledgeMessage parse(short message_id, byte[] data, int offset, int length) {
        if (length != 2) throw new IllegalArgumentException("Message length invalid");
        short request_id = (short) ((data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8));
        return new VCRequestAcknowledgeMessage(message_id, request_id);
    }
}
