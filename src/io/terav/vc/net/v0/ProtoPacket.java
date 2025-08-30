package io.terav.vc.net.v0;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ProtoPacket extends PacketV0 {
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
    protected byte[] data() {
        List<byte[]> mdata = messages.stream().map(m -> m.serialize()).toList();
        int length = 0;
        for (byte[] d : mdata)
            length += d.length;
        byte[] data = new byte[length];
        int j = 0;
        for (byte[] d : mdata) {
            System.arraycopy(d, 0, data, j, d.length);
            j += d.length;
        }
        return data;
    }
    
    @Override
    public String toString() {
        String str = getClass().getSimpleName() + "[packet_id=" + packet_id + ",version=" + Integer.toHexString(proto_ver) + ",messages=(";
        for (Message m : messages)
            str += m.toString() + ",";
        return str.substring(0, str.length() - 1) + ")]";
    }
    
    public static ProtoPacket parse(int packet_id, byte proto_ver, byte[] data, int length) {
        if (length < 12) throw new IllegalArgumentException("Data too short");
        Collection<Message> messages = new ArrayList<>();
        int j = 8;
        while (j < length) {
            messages.add(Message.parse(data, j));
            j += ((data[j] & 0xFF) + 4);
        }
        return new ProtoPacket(packet_id, proto_ver, messages);
    }
}
