package io.terav.vc.net.v1;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

// Two kinds of ProtoPackets in v1:
// - protocol: messages between relevant peers to facilitate proper communication
// - tattle: messages between any and all peers to share information about peers
public final class ProtoPacket extends PacketV1 {
    public final boolean tattle;
    private final Collection<Message> messages;

    public ProtoPacket(int packet_id, byte recipient, boolean tattle, Collection<Message> messages) {
        this(packet_id, PacketV1.VERSION_MINOR, recipient, tattle, messages);
    }
    private ProtoPacket(int packet_id, byte proto_ver, byte recipient, boolean tattle, Collection<Message> messages) {
        super(packet_id, proto_ver, (byte) ((tattle ? 3 : 2) | ((getPriority(messages) & 0xF) << 2)), recipient);
        if (messages.isEmpty()) throw new IllegalArgumentException("Messages collection cannot be empty");
        this.tattle = tattle;
        for (Message m : messages)
            if (m.isTattle() != this.tattle)
                throw new IllegalArgumentException("Cannot include tattle message in protocol packet and vice versa");
        this.messages = Collections.unmodifiableCollection(messages);
    }
    private ProtoPacket(int packet_id, byte proto_ver, byte flags, byte recipient, Collection<Message> messages) {
        super(packet_id, proto_ver, flags, recipient);
        this.tattle = (flags & 0x1) != 0;
        this.messages = messages;
    }
    /**
     * Finds and returns the highest priority from the collection of {@linkplain Message}s.
     * @param messages The collection of Messages to search
     * @return The highest priority found in the messages
     */
    public static final int getPriority(Collection<Message> messages) {
        if (messages == null) throw new NullPointerException();
        if (messages.isEmpty()) throw new IllegalArgumentException("Messages collection cannot be empty");
        int top = 0;
        for (Message m : messages)
            if (m.getPriority() > top) top = m.getPriority();
        return top & 0xF;
    }

    @Override
    protected void serialize(ByteBuffer buffer) {
        for (Message m : messages)
            m.serialize(buffer);
    }
    @Override
    protected int serializedLength() {
        int len = 0;
        for (Message m : messages)
            len += m.serializedLength() + 4;
        return len;
    }
    
    public static ProtoPacket parse(int packet_id, byte flags, byte recipient, ByteBuffer buf) {
        if (buf.remaining() < 4) throw new IllegalArgumentException("Data too short");
        final ArrayList<Message> messages = new ArrayList<>();
        Message message;
        do {
            message = Message.parse(buf);
            if (message != null) messages.add(message);
        } while (message != null);
        return new ProtoPacket(packet_id, buf.get(5), flags, recipient, messages);
    }
}
