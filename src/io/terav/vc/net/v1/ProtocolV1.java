package io.terav.vc.net.v1;

import io.terav.vc.NetworkManager;
import io.terav.vc.net.PeerInfo;
import java.util.HashMap;
import java.util.Random;

public final class ProtocolV1 {
    // key: echo packet data    value: timestamp of outbound packet if positive, if negative it is negative rebound time
    private static final HashMap<Long, Long> pendingEchoes = new HashMap<>();
    // TODO stuf
    private ProtocolV1() {}
    
    public static void activateProtocolProcessor() {
        NetworkManager.addPacketHook((packet, peer) -> {
            if (packet instanceof EchoPacket echo) {
                if (pendingEchoes.containsKey(echo.data) && pendingEchoes.get(echo.data) > 0)
                    pendingEchoes.put(echo.data, pendingEchoes.get(echo.data) - System.currentTimeMillis());
                else
                    NetworkManager.sendPacket(new EchoPacket(peer.nextPacketId(), echo.data), peer.remote);
            }
            return false;
        });
    }
    public static void sendEcho(PeerInfo peer) {
        Random random = new Random();
        long data;
        do {
            data = random.nextLong();
        } while (pendingEchoes.containsKey(data));
        pendingEchoes.put(data, System.currentTimeMillis());
        NetworkManager.sendPacket(new EchoPacket(peer.nextPacketId(), data), peer.remote);
    }
    public static void connectVC(PeerInfo peer, String message) {
        
    }
    
    private static abstract class ConnectionMode extends NetworkManager.ConnectionMode {
        
    }
    private static class SingleConnectionMode extends ConnectionMode {
        
    }
}
