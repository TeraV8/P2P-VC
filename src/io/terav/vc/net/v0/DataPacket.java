package io.terav.vc.net.v0;

import java.nio.ByteBuffer;

public class DataPacket extends PacketV0 {
    public final int channel_id;
    public final int sequence_id;
    public final ByteBuffer data;
    
    public DataPacket(int packet_id, int channel_id, int sequence_id, byte[] data) {
        this(packet_id, PacketV0.VERSION_MINOR, channel_id, sequence_id, data);
    }
    private DataPacket(int packet_id, byte proto_ver, int channel_id, int sequence_id, byte[] data) {
        super(packet_id, proto_ver, (byte) 2);
        if (data == null) throw new NullPointerException();
        this.channel_id = channel_id;
        this.sequence_id = sequence_id;
        this.data = ByteBuffer.wrap(data);
    }
    
    @Override
    protected byte[] data() {
        byte[] superdata = new byte[data.array().length + 8];
        superdata[0] = (byte) channel_id;
        superdata[1] = (byte)(channel_id >> 8);
        superdata[2] = (byte)(channel_id >> 16);
        superdata[3] = (byte)(channel_id >> 24);
        superdata[4] = (byte) sequence_id;
        superdata[5] = (byte)(sequence_id >> 8);
        superdata[6] = (byte)(sequence_id >> 16);
        superdata[7] = (byte)(sequence_id >> 24);
        data.get(0, superdata, 8, data.array().length);
        return superdata;
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[packet_id=" + packet_id + ",version=" + Integer.toHexString(proto_ver) + ",data_length=" + data.array().length + ']';
    }
    
    public static DataPacket parse(int packet_id, byte proto_ver, byte[] data, int length) {
        if (length < 16) throw new IllegalArgumentException("Data too short");
        int channel_id = (data[8] & 0xFF) | ((data[9] & 0xFF) << 8) | ((data[10] & 0xFF) << 16) | ((data[11] & 0xFF) << 24);
        int sequence_id = (data[12] & 0xFF) | ((data[13] & 0xFF) << 8) | ((data[14] & 0xFF) << 16) | ((data[15] & 0xFF) << 24);
        byte[] subdata = new byte[length - 16];
        System.arraycopy(data, 16, subdata, 0, length - 16);
        return new DataPacket(packet_id, proto_ver, channel_id, sequence_id, subdata);
    }
}
