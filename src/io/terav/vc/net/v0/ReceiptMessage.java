package io.terav.vc.net.v0;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class ReceiptMessage extends Message {
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
    protected byte[] data() {
        byte[] data = new byte[exceptions.size() * 4 + 8];
        data[0] = (byte) packet_first;
        data[1] = (byte)(packet_first >> 8);
        data[2] = (byte)(packet_first >> 16);
        data[3] = (byte)(packet_first >> 24);
        data[4] = (byte) packet_last;
        data[5] = (byte)(packet_last >> 8);
        data[6] = (byte)(packet_last >> 16);
        data[7] = (byte)(packet_last >> 24);
        int index = 8;
        for (int num : exceptions) {
            data[index++] = (byte) num;
            data[index++] = (byte)(num >> 8);
            data[index++] = (byte)(num >> 16);
            data[index++] = (byte)(num >> 24);
        }
        return data;
    }
    
    public static ReceiptMessage parse(short message_id, byte[] data, int offset, int length) {
        if (length % 4 != 0 || length < 8) throw new IllegalArgumentException("Message length invalid");
        
        int packet_first = (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8) | ((data[offset + 2] & 0xFF) << 16) | ((data[offset + 3] & 0xFF) << 24);
        int packet_last = (data[offset + 4] & 0xFF) | ((data[offset + 5] & 0xFF) << 8) | ((data[offset + 6] & 0xFF) << 16) | ((data[offset + 7] & 0xFF) << 24);
        Collection<Integer> exceptions = new ArrayList<>();
        for (int i = 0; i * 4 + 8 < length; i++)
            exceptions.add((data[offset + i * 4 + 8] & 0xFF) | ((data[offset + i * 4 + 9] & 0xFF) << 8) | ((data[offset + i * 4 + 10] & 0xFF) << 16) | ((data[offset + i * 4 + 11] & 0xFF) << 24));
        return new ReceiptMessage(message_id, packet_first, packet_last, Collections.unmodifiableCollection(exceptions));
    }
}
