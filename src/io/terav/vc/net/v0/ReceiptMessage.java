package io.terav.vc.net.v0;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public final class ReceiptMessage extends Message {
    public final int packet_first;
    public final int packet_last;
    public final Collection<Integer> exceptions;

    public ReceiptMessage(short message_id, int packet_first, int packet_last, Collection<Integer> exceptions) {
        super((byte) 0xF2, message_id);
        this.packet_first = packet_first;
        this.packet_last = packet_last;
        this.exceptions = Collections.unmodifiableCollection(new ArrayList(exceptions));
    }

    @Override
    protected void serializeMessage(ByteBuffer buffer) {
        buffer.putInt(packet_first);
        buffer.putInt(packet_last);
        for (int exc : exceptions)
            buffer.putInt(exc);
    }
    @Override
    protected int serializedLength() {
        return exceptions.size() * 4 + 8;
    }
    
    public static ReceiptMessage parse(short message_id, ByteBuffer buffer) {
        if (buffer.remaining() % 4 != 0 || buffer.remaining() < 8) throw new IllegalArgumentException("Message length invalid");
        int packet_first = buffer.getInt();
        int packet_last = buffer.getInt();
        Collection<Integer> exceptions = new ArrayList<>();
        while (buffer.hasRemaining())
            exceptions.add(buffer.getInt());
        return new ReceiptMessage(message_id, packet_first, packet_last, Collections.unmodifiableCollection(exceptions));
    }
}
