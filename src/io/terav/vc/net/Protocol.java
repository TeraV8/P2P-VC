package io.terav.vc.net;

import io.terav.vc.net.v0.ProtocolV0;
import io.terav.vc.net.v1.ProtocolV1;

public class Protocol {
    public static final byte HIGHEST_PROTOVER = 1;
    
    private static final boolean[] activatedProtocolProcessors = new boolean[HIGHEST_PROTOVER + 1];
    
    private Protocol() {}
    
    public static byte getVersion(PeerInfo peer) {
        if (peer.protover_compat != -1)
            return (byte) (peer.protover_compat >> 8);
        if (peer.protover_hi == -1)
            return HIGHEST_PROTOVER;
        if (((peer.protover_hi >> 8) & 0xFF) >= HIGHEST_PROTOVER)
            return HIGHEST_PROTOVER;
        peer.protover_compat = (short) ((peer.protover_hi & 0xFF00) | 0xFF);
        return (byte) (peer.protover_hi >> 8);
    }
    public static synchronized void activateProtocolProcessor(byte version) {
        if (version == -1) version = HIGHEST_PROTOVER;
        if ((version & 0xFF) > HIGHEST_PROTOVER) throw new UnsupportedOperationException("Protocol version " + (version & 0xFF) + " not supported");
        if (activatedProtocolProcessors[version]) return;
        switch (version) {
            case 0 -> ProtocolV0.activateProtocolProcessor();
            case 1 -> ProtocolV1.activateProtocolProcessor();
        }
        activatedProtocolProcessors[version] = true;
    }
    public static void sendEcho(PeerInfo peer) {
        switch (getVersion(peer)) {
            case 0 -> ProtocolV0.sendEcho(peer);
            case 1 -> ProtocolV1.sendEcho(peer);
        }
    }
}
