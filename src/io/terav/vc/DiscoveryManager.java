package io.terav.vc;

import io.terav.vc.net.PeerInfo;
import io.terav.vc.net.v0.EchoPacket;
import io.terav.vc.net.v0.ProtocolV0;
import java.util.ConcurrentModificationException;

public class DiscoveryManager implements Runnable {
    public final Thread thread;
    public DiscoveryManager() {
        this.thread = new Thread(this);
        this.thread.setName("DiscoveryManager");
        this.thread.setDaemon(true);
    }
    @Override
    public void run() {
        while (true) {
            final long now = System.currentTimeMillis();
            int nextTime = Integer.MAX_VALUE;
            try {
                for (PeerInfo peer : Main.netman.getPeers()) {
                    if (peer.last_packet_time + 5000 <= now) {
                        ProtocolV0.sendEcho(peer);
                        //Main.netman.sendPacket(new EchoPacket(peer.nextPacketId(), 0x41435449_56495459L), peer.remote);
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
}
