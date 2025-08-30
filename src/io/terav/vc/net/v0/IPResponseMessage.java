package io.terav.vc.net.v0;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class IPResponseMessage extends Message {
    private final byte[] address = new byte[4];

    private IPResponseMessage(short message_id, byte[] address, int index) {
        super((byte) 0xC1, message_id);
        System.arraycopy(address, index, this.address, 0, 4);
    }
    public IPResponseMessage(short message_id, Inet4Address address) {
        this(message_id, address.getAddress(), 0);
    }

    @Override
    protected byte[] data() {
        return address;
    }
    
    public Inet4Address getAddress() {
        try {
            return (Inet4Address) InetAddress.getByAddress(address);
        } catch (UnknownHostException e) {
            // should never happen
            throw new RuntimeException("fault C1");
        }
    }
    
    public static IPResponseMessage parse(short message_id, byte[] data, int offset, int length) {
        if (length != 4) throw new IllegalArgumentException("Message length invalid");
        return new IPResponseMessage(message_id, data, offset);
    }
}
