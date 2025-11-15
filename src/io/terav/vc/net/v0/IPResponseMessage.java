package io.terav.vc.net.v0;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class IPResponseMessage extends Message {
    private final byte[] address = new byte[4];

    private IPResponseMessage(short message_id, ByteBuffer buffer) {
        super((byte) 0xC1, message_id);
        buffer.get(address);
    }
    public IPResponseMessage(short message_id, Inet4Address address) {
        super((byte) 0xC1, message_id);
        System.arraycopy(address.getAddress(), 0, this.address, 0, 4);
    }

    @Override
    protected void serializeMessage(ByteBuffer buffer) {
        buffer.put(address);
    }
    @Override
    protected int serializedLength() {
        return 4;
    }
    
    public Inet4Address getAddress() {
        try {
            return (Inet4Address) InetAddress.getByAddress(address);
        } catch (UnknownHostException e) {
            // should never happen
            throw new RuntimeException("fault C1");
        }
    }
    
    public static IPResponseMessage parse(short message_id, ByteBuffer buffer) {
        if (buffer.remaining() != 4) throw new IllegalArgumentException("Message length invalid");
        return new IPResponseMessage(message_id, buffer);
    }
}
