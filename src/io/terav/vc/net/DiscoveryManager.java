package io.terav.vc.net;

import io.terav.vc.NetworkManager;
import io.terav.vc.net.v0.ProtocolV0;
import java.util.ConcurrentModificationException;

public class DiscoveryManager implements Runnable {
    private static Thread thread;
    private DiscoveryManager() {}
    @Override
    public void run() {
        while (true) {
            final long now = System.currentTimeMillis();
            int nextTime = Integer.MAX_VALUE;
            try {
                for (PeerInfo peer : NetworkManager.getPeers()) {
                    if (peer.last_packet_time + 5000 <= now) {
                        ProtocolV0.sendEcho(peer);
                        peer.last_packet_time = now;
                        nextTime = Math.min(nextTime, 5000);
                    } else {
                        nextTime = Math.min((int) (peer.last_packet_time + 5000 - now), nextTime);
                    }
                }
            } catch (ConcurrentModificationException e) {
                // not a big deal
                continue;
            }
            try {
                Thread.sleep(nextTime);
            } catch (InterruptedException e) {}
        }
    }
    public static void interrupt() {
        if (thread == null || !thread.isAlive()) return;
        thread.interrupt();
    }
    public static synchronized void start() {
        if (thread != null) throw new IllegalStateException("DiscoveryManager is already running");
        thread = new Thread(new DiscoveryManager());
        thread.setName("DiscoveryManager");
        thread.setDaemon(true);
        thread.start();
    }
}
