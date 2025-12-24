package io.terav.vc.net.v1;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

public abstract class Message {
    /**
     * <table>
     * <tr><td>0x74</td><td>{@link PeerSelfIdentifyMessage} (protocol)</td></tr>
     * <tr><td>0x75</td><td>{@link EmptyMessage#PROTO_REQUEST_IDENTIFY}</td></tr>
     * <tr><td>0x7E</td><td>{@link EmptyMessage#PROTO_DOWNGRADE}</td></tr>
     * <tr><td>0x7F</td><td>Reserved (protocol)</td></tr>
     * <tr><td>0xF4</td><td>{@link PeerSelfIdentifyMessage} (tattle)</td></tr>
     * <tr><td>0xFF</td><td>Reserved (tattle)</td></tr>
     * </table>
     */
    public final byte type;
    public final short message_id;
    
    protected Message(byte type, short message_id) {
        this.type = type;
        this.message_id = message_id;
    }
    
    public boolean isTattle() {
        return (this.type & 0x80) != 0;
    }
    /**
     * <p>Return the routing priority of this message.</p>
     * Messages with a higher priority number should be routed sooner.
     * 
     * <ul><li>Routing priority must not &gt; 15 or &lt; 0</li>
     * <li>Routing priority &lt; 8 should not be forwarded (generally set this for protocol messages)</li>
     * <li>Routing priority == 15 is reserved for future use</li></ul>
     * @return The routing priority of this message
     */
    public int getPriority() {
        return 3 | ((this.type & 0x80) >> 4);
    }
    
    protected abstract void serializeContents(ByteBuffer buf);
    protected abstract int serializedLength();
    public final void serialize(ByteBuffer buf) {
        int length = serializedLength();
        if (length > 255) throw new IllegalStateException("Message data too long");
        buf.putShort(message_id);
        buf.put(type);
        buf.put((byte) length);
        this.serializeContents(buf.slice(buf.position(), length));
        buf.position(buf.position() + length);
    }
    
    public static Message parse(ByteBuffer buf) {
        try {
            short message_id = buf.getShort();
            byte type = buf.get();
            int length = buf.get() & 0xFF;
            ByteBuffer mini = buf.slice(buf.position(), length);
            buf.position(buf.position() + length);
            return switch (type & 0xFF) {
                case 0x74, 0xF4 -> PeerSelfIdentifyMessage.parse(type, message_id, mini);
                case 0x75, 0x7E -> new EmptyMessage(type, message_id);
                default -> new InvalidMessage(message_id, type);
            };
        } catch (BufferUnderflowException | IndexOutOfBoundsException e) {
            return null;
        }
    }
}
