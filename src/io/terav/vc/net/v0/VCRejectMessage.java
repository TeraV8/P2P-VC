package io.terav.vc.net.v0;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class VCRejectMessage extends Message {
    public final short request_id;
    public final String note;

    public VCRejectMessage(short message_id, short request_id, String note) {
        super((byte) 0x33, message_id);
        this.request_id = request_id;
        this.note = note;
    }

    @Override
    protected void serializeMessage(ByteBuffer buffer) {
        buffer.putShort(request_id);
        buffer.put(note.getBytes(Charset.forName("ISO-8859-1")));
    }
    @Override
    protected int serializedLength() {
        return note.getBytes(Charset.forName("ISO-8859-1")).length + 2;
    }
    
    public static VCRejectMessage parse(short message_id, ByteBuffer buffer) {
        if (buffer.remaining() < 2) throw new IllegalArgumentException("Message length too short");
        return new VCRejectMessage(message_id, buffer.getShort(), new String(buffer.array(), buffer.position(), buffer.remaining(), Charset.forName("ISO-8859-1")));
    }
}
