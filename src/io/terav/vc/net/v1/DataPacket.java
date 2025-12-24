package io.terav.vc.net.v1;

import java.nio.ByteBuffer;

public class DataPacket extends PacketV1 {
    public final int channel_id;
    public final int sequence;
    private final byte[] data;
    
    public DataPacket(int packet_id, int channel_id, int sequence, byte[] data) {
        this(packet_id, (byte) 0, channel_id, sequence, data);
    }
    public DataPacket(int packet_id, byte recipient, int channel_id, int sequence, byte[] data) {
        this(packet_id, PacketV1.VERSION_MINOR, recipient, channel_id, sequence, data);
    }
    private DataPacket(int packet_id, byte version, byte recipient, int channel_id, int sequence, byte[] data) {
        super(packet_id, version, (byte) 1, recipient);
        if (data == null) throw new NullPointerException();
        this.channel_id = channel_id;
        this.sequence = sequence;
        this.data = data;
    }
    
    @Override
    protected void serialize(ByteBuffer buffer) {
        buffer.putInt(channel_id);
        buffer.putInt(sequence);
        buffer.put(data);
    }
    @Override
    protected int serializedLength() {
        return 8 + data.length;
    }
    
    public static DataPacket parse(int packet_id, byte recipient, ByteBuffer buffer) {
        if (buffer.remaining() < 8) throw new IllegalArgumentException("Data too short");
        final int channel_id = buffer.getInt();
        final int sequence = buffer.getInt();
        final byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        return new DataPacket(packet_id, recipient, channel_id, sequence, data);
    }
}
