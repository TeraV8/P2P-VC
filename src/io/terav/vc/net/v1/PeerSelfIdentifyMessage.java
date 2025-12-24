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
public class PeerSelfIdentifyMessage extends Message {
    public final UUID selfId;
    public final String commonName;

    public PeerSelfIdentifyMessage(boolean tattle, short message_id, UUID uuid, String name) {
        super((byte) (tattle ? 0xF4 : 0x74), message_id);
        this.selfId = (uuid == null) ? new UUID(0L, 0L) : uuid;
        this.commonName = (name == null) ? "" : name;
    }

    @Override
    protected void serializeContents(ByteBuffer buf) {
        buf.putLong(selfId.getMostSignificantBits());
        buf.putLong(selfId.getLeastSignificantBits());
        buf.put(commonName.getBytes(Charset.forName("ISO-8859-1")));
    }
    @Override
    protected int serializedLength() {
        return 16 + commonName.getBytes(Charset.forName("ISO-8859-1")).length;
    }
    
    public static PeerSelfIdentifyMessage parse(byte type, short message_id, ByteBuffer buf) {
        final UUID uuid = new UUID(buf.getLong(), buf.getLong());
        final byte[] nameBytes = new byte[buf.remaining()];
        buf.get(nameBytes);
        return new PeerSelfIdentifyMessage((type & 0x80) != 0, message_id, uuid, new String(nameBytes, Charset.forName("ISO-8859-1")));
    }
}
