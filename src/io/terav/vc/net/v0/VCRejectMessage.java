package io.terav.vc.net.v0;

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
    protected byte[] data() {
        byte[] noteBytes = note.getBytes(Charset.forName("ISO-8859-1"));
        byte[] data = new byte[noteBytes.length + 2];
        data[0] = (byte) request_id;
        data[1] = (byte)(request_id >> 8);
        System.arraycopy(noteBytes, 0, data, 2, noteBytes.length);
        return data;
    }
    
    public static VCRejectMessage parse(short message_id, byte[] data, int offset, int length) {
        if (length < 2) throw new IllegalArgumentException("Message length too short");
        short request_id = (short) ((data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8));
        return new VCRejectMessage(message_id, request_id, new String(data, offset + 2, length - 2, Charset.forName("ISO-8859-1")));
    }
}
