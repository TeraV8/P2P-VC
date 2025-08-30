package io.terav.vc.net.v0;

public class VCAcceptMessage extends Message {
    public final int remote_channel;
    public final short request_id;

    public VCAcceptMessage(short message_id, int remote_channel, short request_id) {
        super((byte) 0x32, message_id);
        this.remote_channel = remote_channel;
        this.request_id = request_id;
    }

    @Override
    protected byte[] data() {
        byte[] data = new byte[6];
        data[0] = (byte) remote_channel;
        data[1] = (byte)(remote_channel >> 8);
        data[2] = (byte)(remote_channel >> 16);
        data[3] = (byte)(remote_channel >> 24);
        data[4] = (byte) request_id;
        data[5] = (byte)(request_id >> 8);
        return data;
    }
    
    public static VCAcceptMessage parse(short message_id, byte[] data, int offset, int length) {
        if (length != 6) throw new IllegalArgumentException("Message length invalid");
        int remote_channel = (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8) | ((data[offset + 2] & 0xFF) << 16) | (data[offset + 3] << 24);
        short request_id = (short) ((data[offset + 4] & 0xFF) | ((data[offset + 5] & 0xFF) << 8));
        return new VCAcceptMessage(message_id, remote_channel, request_id);
    }
}
