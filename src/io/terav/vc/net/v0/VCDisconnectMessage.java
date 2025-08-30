package io.terav.vc.net.v0;

public class VCDisconnectMessage extends Message {
    public VCDisconnectMessage(short message_id) {
        super((byte) 0x34, message_id);
    }
    @Override
    protected byte[] data() {
        return new byte[0];
    }
    public static VCDisconnectMessage parse(short message_id, byte[] data, int offset, int length) {
        if (length != 0) throw new IllegalArgumentException("Message length invalid");
        return new VCDisconnectMessage(message_id);
    }
}
