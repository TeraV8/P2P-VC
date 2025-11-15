package io.terav.vc.net.v0;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class VCRequestMessage extends Message {
    public final String note;

    public VCRequestMessage(short message_id, String note) {
        super((byte) 0x30, message_id);
        this.note = note;
    }

    @Override
    protected void serializeMessage(ByteBuffer buffer) {
        buffer.put(note.getBytes(Charset.forName("ISO-8859-1")));
    }
    @Override
    protected int serializedLength() {
        return note.getBytes(Charset.forName("ISO-8859-1")).length;
    }
    
    public static VCRequestMessage parse(short message_id, ByteBuffer buffer) {
        return new VCRequestMessage(message_id, new String(buffer.array(), buffer.position(), buffer.remaining(), Charset.forName("ISO-8859-1")));
    }
}
