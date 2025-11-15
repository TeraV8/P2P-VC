package io.terav.vc.net.v0;

import java.nio.ByteBuffer;

public class VCAcceptMessage extends Message {
    public final int remote_channel;
    public final short request_id;

    public VCAcceptMessage(short message_id, int remote_channel, short request_id) {
        super((byte) 0x32, message_id);
        this.remote_channel = remote_channel;
        this.request_id = request_id;
    }

    @Override
    protected void serializeMessage(ByteBuffer buffer) {
        buffer.putInt(remote_channel);
        buffer.putShort(request_id);
    }
    @Override
    protected int serializedLength() {
        return 6;
    }
    
    public static VCAcceptMessage parse(short message_id, ByteBuffer buffer) {
        if (buffer.remaining() != 6) throw new IllegalArgumentException("Message length invalid");
        return new VCAcceptMessage(message_id, buffer.getInt(), buffer.getShort());
    }
}
