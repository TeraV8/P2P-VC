package io.terav.vc;

import io.terav.vc.net.Packet;
import io.terav.vc.net.PacketReceiver;
import io.terav.vc.net.PeerInfo;
import io.terav.vc.net.v0.ProtocolV0;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiPredicate;
import javax.swing.JOptionPane;

public class NetworkManager implements Runnable {
    public ConnectionMode connectionMode = null;
    private final DiscoveryManager discoman;
    private final DatagramSocket socket;
    public final Thread thread;
    private final ConcurrentLinkedQueue<Runnable> tasks = new ConcurrentLinkedQueue<>();
    private final PacketReceiver pakr;
    private boolean running = true; // you'd better go catch it then
    private final Map<InetAddress, PeerInfo> peers = Collections.synchronizedMap(new HashMap<>());
    private final List<BiPredicate<Packet,PeerInfo>> hooks = Collections.synchronizedList(new ArrayList<>());
    
    private NetworkManager() {
        try {
            this.socket = new DatagramSocket(31416, InetAddress.getByAddress(new byte[4]));
        } catch (SocketException | UnknownHostException ex) {
            throw new RuntimeException("Could not open socket", ex);
        }
        this.pakr = new PacketReceiver(this.socket);
        this.thread = new Thread(this);
        this.thread.setName("NetworkManager");
        this.thread.setDaemon(true);
        this.discoman = new DiscoveryManager();
    }
    
    @Override
    public void run() {
        discoman.thread.start();
        while (running) {
            while (tasks.isEmpty()) {
                Entry<InetAddress,Packet> p;
                long now;
                try {
                    p = pakr.packets.take();
                    now = System.currentTimeMillis();
                } catch (InterruptedException e) {
                    break;
                }
                final PeerInfo peer = getPeer(p.getKey());
                peer.last_receipt_time = now;
                for (int i = 0; i < hooks.size(); i++)
                    if (hooks.get(i).test(p.getValue(), peer))
                        hooks.remove(i--);
            }
            Runnable task = tasks.poll();
            if (task == null) continue;
            task.run();
        }
    }
    
    public PeerInfo getPeer(InetAddress remote) {
        PeerInfo pi = peers.get(remote);
        if (pi != null) return pi;
        pi = new PeerInfo(remote);
        peers.put(remote, pi);
        Main.window.peersUpdate();
        discoman.thread.interrupt();
        return pi;
    }
    public Collection<PeerInfo> getPeers() {
        return Collections.unmodifiableCollection(peers.values());
    }
    public void addPacketHook(BiPredicate<Packet,PeerInfo> hook) {
        if (hook == null) throw new NullPointerException();
        hooks.add(hook);
    }
    public void addTask(Runnable task) {
        tasks.add(task);
        thread.interrupt();
    }
    
    public void sendPacket(Packet p, InetAddress dest) {
        addTask(new SendPacketTask(p, dest));
    }
    public void connectVC(InetAddress remote, String note) {
        if (connectionMode != null && !connectionMode.mode.finalized)
            ProtocolV0.disconnect();
        ProtocolV0.connectVC(getPeer(remote), note);
    }
    public void disconnectVC() {
        ProtocolV0.disconnect();
    }
    public PeerInfo getConnectedPeer() {
        if (connectionMode == null) return null;
        if (connectionMode.mode.finalized) return null;
        return connectionMode.peer;
    }
    
    void registerPeer(PeerInfo pi, boolean suppressUpdate) {
        peers.put(pi.remote, pi);
        discoman.thread.interrupt();
        if (!suppressUpdate) Main.window.peersUpdate();
    }
    void forgetPeer(PeerInfo peer) {
        if (!peers.containsValue(peer)) return;
        if (getConnectedPeer() == peer)
            disconnectVC();
        peers.remove(peer.remote);
        Main.window.peersUpdate();
        ConfigManager.deletePeerConfig(peer);
    }
    void stop() {
        disconnectVC();
        running = false;
        thread.interrupt();
    }
    
    static NetworkManager start() {
        NetworkManager nm;
        try {
            nm = new NetworkManager();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof BindException)
                JOptionPane.showMessageDialog(null, "Failed to open socket!\nP2P-VC may already be running.\nClose other instances, then try again.", "Failed to open socket", JOptionPane.ERROR_MESSAGE);
            throw e;
        }
        nm.pakr.thread.start();
        Packet.activateProtocolProcessor((byte) -1, nm);
        return nm;
    }

    public static abstract class ConnectionMode {
        public Mode mode;
        public PeerInfo peer;

        public ConnectionMode() {}
        public static enum Mode {
            /** A request has been made to the remote peer and has not yet been acknowledged */
            Connecting(false),
            /** The request did not successfully reach the remote peer (connection aborted) */
            Failed(true),
            /** The request has been acknowledged by the remote peer and we are awaiting user response */
            Waiting(false),
            /** The request has been denied by the user */
            Rejected(true),
            /** The connection is established and data is being transmitted and received */
            Connected(false),
            /** The connection has been terminated by the remote peer */
            Disconnected(true);
            /** If false, ongoing network traffic with this peer is expected */
            public final boolean finalized;
            private Mode(boolean finalized) {
                this.finalized = finalized;
            }
        }
    }
    
    public class SendPacketTask implements Runnable {
        private final Packet packet;
        private final InetAddress remote;
        private int attempts = 0;
        SendPacketTask(Packet p, InetAddress remote) {
            this.packet = p;
            this.remote = remote;
        }
        @Override
        public void run() {
            try {
                byte[] data = packet.serialize();
                DatagramPacket p = new DatagramPacket(data, data.length);
                p.setAddress(remote);
                p.setPort(31416);
                for (int i = 0; i < 3; i++) {
                    socket.send(p);
                    
                }
            } catch (SocketException e) {
                if (!socket.isClosed())
                    System.err.println("Failed to send packet to " + remote.getHostAddress() + ": " + e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                if (++attempts < 5)
                    tasks.add(this);
            }
        }
    }
}
