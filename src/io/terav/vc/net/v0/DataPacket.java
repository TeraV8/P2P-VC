package io.terav.vc.net.v0;

import java.nio.ByteBuffer;

public final class DataPacket extends PacketV0 {
    public final int channel_id;
    public final int sequence_id;
    public final byte[] data;
    
    public DataPacket(int packet_id, int channel_id, int sequence_id, byte[] data) {
        this(packet_id, PacketV0.VERSION_MINOR, channel_id, sequence_id, data);
    }
    private DataPacket(int packet_id, byte proto_ver, int channel_id, int sequence_id, byte[] data) {
        super(packet_id, proto_ver, (byte) 2);
        if (data == null) throw new NullPointerException();
        this.channel_id = channel_id;
        this.sequence_id = sequence_id;
        this.data = data;
    }
    
    @Override
    protected void serialize(ByteBuffer buffer) {
        buffer.putInt(channel_id);
        buffer.putInt(sequence_id);
        buffer.put(data);
    }
    @Override
    protected int serializedLength() {
        return 8 + data.length;
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[packet_id=" + packet_id + ",version=" + Integer.toHexString(proto_ver) + ",data_length=" + data.length + ']';
    }
    
    public static DataPacket parse(int packet_id, byte proto_ver, ByteBuffer buffer) {
        if (buffer.remaining() < 8) throw new IllegalArgumentException("Data too short");
        final int channel_id = buffer.getInt();
        final int sequence_id = buffer.getInt();
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        return new DataPacket(packet_id, proto_ver, channel_id, sequence_id, data);
    }
}
