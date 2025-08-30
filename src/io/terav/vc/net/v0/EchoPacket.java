package io.terav.vc.net.v0;

public class EchoPacket extends PacketV0 {
    public final long content;

    public EchoPacket(int packet_id, long content) {
        this(packet_id, PacketV0.VERSION_MINOR, content);
    }
    private EchoPacket(int packet_id, byte proto_ver, long content) {
        super(packet_id, proto_ver, (byte) 0);
        this.content = content;
    }

    @Override
    protected byte[] data() {
        byte[] data = new byte[8];
        data[0] = (byte) content;
        data[1] = (byte)(content >> 8);
        data[2] = (byte)(content >> 16);
        data[3] = (byte)(content >> 24);
        data[4] = (byte)(content >> 32);
        data[5] = (byte)(content >> 40);
        data[6] = (byte)(content >> 48);
        data[7] = (byte)(content >> 56);
        return data;
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[packet_id=" + packet_id + ",version=" + Integer.toHexString(proto_ver) + ",content=" + Long.toHexString(content) + ']';
    }
    
    public static EchoPacket parse(int packet_id, byte proto_ver, byte[] data, int length) {
        if (length != 16) throw new IllegalArgumentException("Data length mismatch");
        long content = 0;
        for (int i = 0; i < 8; i++)
            content |= (long)(data[i + 8] & 0xFF) << (i * 8);
        return new EchoPacket(packet_id, proto_ver, content);
    }
}
