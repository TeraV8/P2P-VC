package io.terav.vc.net.v0;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public final class ProtoPacket extends PacketV0 {
    public final Collection<Message> messages;

    public ProtoPacket(int packet_id, Collection<Message> messages) {
        this(packet_id, PacketV0.VERSION_MINOR, messages);
    }
    private ProtoPacket(int packet_id, byte proto_ver, Collection<Message> messages) {
        super(packet_id, proto_ver, (byte) 1);
        if (messages.isEmpty()) throw new IllegalArgumentException("Packet must contain at least one message");
        this.messages = Collections.unmodifiableCollection(messages);
    }

    @Override
    protected void serialize(ByteBuffer buffer) {
        for (Message m : messages) {
            int nextPosition = buffer.position() + m.serializedLength() + 4;
            m.serialize(buffer.slice(buffer.position(), m.serializedLength() + 4).order(ByteOrder.LITTLE_ENDIAN));
            buffer.position(nextPosition);
        }
    }
    @Override
    protected int serializedLength() {
        int length = 0;
        for (Message m : messages)
            length += m.serializedLength() + 4;
        return length;
    }
    
    @Override
    public String toString() {
        String str = getClass().getSimpleName() + "[packet_id=" + packet_id + ",version=" + Integer.toHexString(proto_ver) + ",messages=(";
        for (Message m : messages)
            str += m.toString() + ",";
        return str.substring(0, str.length() - 1) + ")]";
    }
    
    public static ProtoPacket parse(int packet_id, byte proto_ver, ByteBuffer buffer) {
        if (buffer.remaining() < 4) throw new IllegalArgumentException("Data too short");
        Collection<Message> messages = new ArrayList<>();
        while (buffer.hasRemaining())
            messages.add(Message.parse(buffer));
        return new ProtoPacket(packet_id, proto_ver, messages);
    }
}
