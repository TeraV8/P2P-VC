package io.terav.vc.net.v1;

import java.nio.ByteBuffer;

public class EchoPacket extends PacketV1 {

    public EchoPacket(int packet_id) {
        this(packet_id, PacketV1.VERSION_MINOR);
    }
    public EchoPacket(int packet_id, byte proto_ver) {
        super(packet_id, proto_ver, (byte) 0, (byte) 0);
    }
    
    @Override
    protected void serialize(ByteBuffer buffer) {}
    @Override
    protected int serializedLength() {
        return 0;
    }
}
