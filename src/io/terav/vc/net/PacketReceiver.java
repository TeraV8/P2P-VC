package io.terav.vc.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.LinkedBlockingQueue;

public final class PacketReceiver implements Runnable {
    public final LinkedBlockingQueue<Entry<InetAddress,Packet>> packets = new LinkedBlockingQueue<>();
    public final Thread thread;
    private final DatagramSocket socket;
    private final HashMap<InetAddress, Integer> last_received_packet = new HashMap<>();
    
    public PacketReceiver(DatagramSocket socket) {
        if (socket == null) throw new NullPointerException();
        this.socket = socket;
        this.thread = new Thread(this);
        this.thread.setName("PacketReceiver");
        this.thread.setDaemon(true);
    }
    
    @Override
    public void run() {
        DatagramPacket packet = new DatagramPacket(new byte[65504], 65504);
        while (!socket.isClosed()) {
            try {
                packet.setLength(65504);
                socket.receive(packet);
                Packet p = Packet.parse(packet.getData(), packet.getLength());
                if (last_received_packet.getOrDefault(packet.getAddress(), -1) != p.packet_id) {
                    packets.add(new SimpleEntry(packet.getAddress(), p));
                    last_received_packet.put(packet.getAddress(), p.packet_id);
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }
}
