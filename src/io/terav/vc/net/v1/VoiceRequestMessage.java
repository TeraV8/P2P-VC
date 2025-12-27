package io.terav.vc.net.v1;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Request a voice connection to the peer.<br>
 * A short message string may be sent to be shown to the user.
 * 
 * <p>Message type {@code 0x60}</p>
 * @author terav
 */
public final class VoiceRequestMessage extends Message {
    public final String message;
    private final byte[] message_bytes;

    public VoiceRequestMessage(short message_id, String message) {
        super((byte) 0x60, message_id);
        this.message = (message == null) ? "" : message;
        this.message_bytes = this.message.getBytes(Charset.forName("ISO-8859-1"));
        if (serializedLength() > 255) throw new IllegalArgumentException("Message too long (limit 255 bytes)");
    }
    private VoiceRequestMessage(short message_id, byte[] messageBytes) {
        super((byte) 0x60, message_id);
        this.message_bytes = messageBytes;
        this.message = new String(messageBytes, Charset.forName("ISO-8859-1"));
    }

    @Override
    protected void serializeContents(ByteBuffer buf) {
        buf.put(message_bytes);
    }
    @Override
    protected int serializedLength() {
        return message_bytes.length;
    }
    
    public static VoiceRequestMessage parse(short message_id, ByteBuffer buf) {
        final byte[] messageBytes = new byte[buf.remaining()];
        buf.get(messageBytes);
        return new VoiceRequestMessage(message_id, messageBytes);
    }
}
