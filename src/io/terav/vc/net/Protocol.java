package io.terav.vc.net;

import io.terav.vc.net.v0.ProtocolV0;
import io.terav.vc.net.v1.ProtocolV1;

public class Protocol {
    public static final byte HIGHEST_PROTOVER = 1;
    
    private static final boolean[] activatedProtocolProcessors = new boolean[HIGHEST_PROTOVER + 1];
    
    private Protocol() {}
    
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
}
