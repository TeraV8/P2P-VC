package io.terav.vc.net.v0;

public class IPRequestMessage extends Message {

    public IPRequestMessage(short message_id) {
        super((byte) 0xC0, message_id);
    }

    @Override
    protected byte[] data() {
        return new byte[0];
    }
    
    public static IPRequestMessage parse(short message_id, byte[] data, int offset, int length) {
        if (length > 0) throw new IllegalArgumentException("Message length invalid");
        return new IPRequestMessage(message_id);
    }
}
