package io.terav.vc.net.v0;

public class ProtoDowngradeMessage extends Message {
    public final short version;

    public ProtoDowngradeMessage(short message_id) {
        this(message_id, (short)(PacketV0.VERSION_MINOR & 0xFF));
    }
    private ProtoDowngradeMessage(short message_id, short version) {
        super((byte) 0xF0, message_id);
        this.version = version;
    }

    @Override
    protected byte[] data() {
        byte[] data = new byte[2];
        data[0] = (byte) version;
        data[1] = (byte)(version >> 8);
        return data;
    }
    
    public static ProtoDowngradeMessage parse(short message_id, byte[] data, int offset, int length) {
        if (length != 2) throw new IllegalArgumentException("Message length invalid");
        short version = (short) ((data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8));
        return new ProtoDowngradeMessage(message_id, version);
    }
}
