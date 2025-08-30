package io.terav.vc.net.v0;

import java.nio.charset.Charset;

public class VCRequestMessage extends Message {
    public final String note;

    public VCRequestMessage(short message_id, String note) {
        super((byte) 0x30, message_id);
        this.note = note;
    }

    @Override
    protected byte[] data() {
        return note.getBytes(Charset.forName("ISO-8859-1"));
    }
    // TODO stuf
    public static VCRequestMessage parse(short message_id, byte[] data, int offset, int length) {
        return new VCRequestMessage(message_id, new String(data, offset, length, Charset.forName("ISO-8859-1")));
    }
}
