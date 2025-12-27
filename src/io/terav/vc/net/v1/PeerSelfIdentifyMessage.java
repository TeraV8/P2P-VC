package io.terav.vc.net.v1;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.UUID;

/**
 * Notify the peer(s) of this client's UUID and common name. Can be sent as either protocol or tattle.<br>
 * {@code null} may be specified for either parameter if missing or private. NOTE: Peers may refuse to accept connections from anonymous peers.
 * 
 * <p>Message type {@code 0x74} (protocol) or {@code 0xF4} (tattle)</p>
 * 
 * @author terav
 */
public final class PeerSelfIdentifyMessage extends Message {
    public final UUID selfId;
    public final String commonName;
    private final byte[] commonName_bytes;

    public PeerSelfIdentifyMessage(boolean tattle, short message_id, UUID uuid, String name) {
        super((byte) (tattle ? 0xF4 : 0x74), message_id);
        this.selfId = (uuid == null) ? new UUID(0L, 0L) : uuid;
        this.commonName = (name == null) ? "" : name;
        this.commonName_bytes = this.commonName.getBytes(Charset.forName("ISO-8859-1"));
        if (serializedLength() > 255) throw new IllegalArgumentException("Common name is too long (limit 239 bytes)");
    }
    private PeerSelfIdentifyMessage(byte type, short message_id, UUID uuid, byte[] nameBytes) {
        super(type, message_id);
        this.selfId = uuid;
        this.commonName_bytes = nameBytes;
        this.commonName = new String(nameBytes, Charset.forName("ISO-8859-1"));
    }

    @Override
    protected void serializeContents(ByteBuffer buf) {
        buf.putLong(selfId.getMostSignificantBits());
        buf.putLong(selfId.getLeastSignificantBits());
        buf.put(commonName_bytes);
    }
    @Override
    protected int serializedLength() {
        return 16 + commonName_bytes.length;
    }
    
    public static PeerSelfIdentifyMessage parse(byte type, short message_id, ByteBuffer buf) {
        final UUID uuid = new UUID(buf.getLong(), buf.getLong());
        final byte[] nameBytes = new byte[buf.remaining()];
        buf.get(nameBytes);
        return new PeerSelfIdentifyMessage(type, message_id, uuid, nameBytes);
    }
}
