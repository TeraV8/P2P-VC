package io.terav.vc.net.v1;

import io.terav.vc.NetworkManager;
import io.terav.vc.net.PeerInfo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Random;

public final class ProtocolV1 {
    // key: echo packet data    value: timestamp of outbound packet if positive, if negative it is negative rebound time
    private static final HashMap<Long, Long> pendingEchoes = new HashMap<>();
    private static final HashMap<PeerInfo, ArrayList<Message>> pendingOutbound = new HashMap<>();
    private static final HashMap<PeerInfo, ArrayList<Message>> pendingTattle = new HashMap<>();
    
    private static NetworkSynchronizer sync;
    // TODO stuf
    private ProtocolV1() {}
    
    public static void activateProtocolProcessor() {
        NetworkManager.addPacketHook((packet, peer) -> {
            if (packet instanceof EchoPacket echo) {
                if (pendingEchoes.containsKey(echo.data) && pendingEchoes.get(echo.data) > 0) {
                    long time = pendingEchoes.get(echo.data) - System.currentTimeMillis();
                    peer.ping = time;
                    peer.last_echo_time = System.currentTimeMillis();
                    pendingEchoes.remove(echo.data);
                } else
                    NetworkManager.sendPacket(new EchoPacket(peer.nextPacketId(), echo.data), peer.remote);
            }
            return false;
        });
        sync = new NetworkSynchronizer();
        sync.thread.start();
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
    
    public static void queueMessage(PeerInfo peer, Message message) {
        if (message.isTattle()) {
            synchronized (pendingTattle) {
                if (!pendingTattle.containsKey(peer))
                    pendingTattle.put(peer, new ArrayList<>());
                pendingTattle.get(peer).add(message);
            }
        } else {
            synchronized (pendingOutbound) {
                if (!pendingOutbound.containsKey(peer))
                    pendingOutbound.put(peer, new ArrayList<>());
                pendingOutbound.get(peer).add(message);
            }
        }
    }
    
    private static abstract class ConnectionMode extends NetworkManager.ConnectionMode {
        public int sequence = 1;
    }
    private static final class SingleConnectionMode extends ConnectionMode {
        public PeerInfo remote;
        
        @Override
        public void disconnectProto() {
            // TODO
        }
        @Override
        public boolean includes(PeerInfo peer) {
            return remote == peer;
        }
    }
    private static final class JointConnectionMode extends ConnectionMode {
        public final Collection<PeerInfo> remote = new HashSet<>();
        
        @Override
        public void disconnectProto() {
            // TODO
        }
        @Override
        public boolean includes(PeerInfo peer) {
            return remote.contains(peer);
        }
    }
    
    private static final class NetworkSynchronizer implements Runnable {
        private static final int MAX_MESSAGE_DELAY_TIME = 200;
        
        final Thread thread;
        NetworkSynchronizer() {
            this.thread = new Thread(this, "proto-v1-sync");
            this.thread.setDaemon(true);
        }
        @Override
        public void run() {
            while (true) {
                synchronized (pendingOutbound) {
                    for (Entry<PeerInfo, ArrayList<Message>> entry : pendingOutbound.entrySet()) {
                        if (!entry.getValue().isEmpty())
                            entry.getKey().send(new ProtoPacket(entry.getKey().nextPacketId(), (byte) 0, false, entry.getValue()));
                        entry.getValue().clear();
                    }
                }
                synchronized (pendingTattle) {
                    for (Entry<PeerInfo, ArrayList<Message>> entry : pendingTattle.entrySet()) {
                        if (!entry.getValue().isEmpty())
                            entry.getKey().send(new ProtoPacket(entry.getKey().nextPacketId(), (byte) 0, true, entry.getValue()));
                        entry.getValue().clear();
                    }
                }
                try {
                    Thread.sleep(MAX_MESSAGE_DELAY_TIME);
                } catch (InterruptedException e) {}
            }
        }
    }
}
