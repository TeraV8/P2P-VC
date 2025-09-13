package io.terav.vc.net.v0;

import io.terav.vc.AudioManager;
import io.terav.vc.Main;
import io.terav.vc.NetworkManager;
import io.terav.vc.StreamOutputDriver;
import io.terav.vc.net.PeerInfo;
import java.net.Inet4Address;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import javax.swing.JOptionPane;

public final class ProtocolV0 {
    private static final HashMap<Long, Long> pending_echoes = new HashMap<>();
    private static final HashMap<Long, Long> downgrade_request_times = new HashMap<>();
    // TODO stuf
    private ProtocolV0() {}
    public static void activateProtocolProcessor() {
        NetworkManager.addPacketHook((nvp,peer) -> {
            ConnectionMode cm = null;
            if (NetworkManager.connectionMode != null && (NetworkManager.connectionMode instanceof ConnectionMode)) cm = (ConnectionMode) NetworkManager.connectionMode;
            final ConnectionMode fcm = cm; // workaround for finicky fields in lambda expressions
            if (nvp instanceof EchoPacket p) {
                if (pending_echoes.containsKey(p.content))
                    pending_echoes.remove(p.content);
                else
                    peer.send(new EchoPacket(peer.nextPacketId(), p.content));
            } else if (nvp instanceof ProtoPacket p) {
                for (Message m : p.messages) {
                    if (m instanceof VCRequestMessage vcrq) {
                        // send an acknowledgement posthaste
                        NetworkManager.sendPacket(new ProtoPacket(peer.nextPacketId(), Arrays.asList(new VCRequestAcknowledgeMessage(peer.nextMessageId(), m.message_id))), peer.remote);
                        Main.window.tasks.add(() -> {
                            int result = JOptionPane.showOptionDialog(
                                    Main.window,
                                    new String[] { peer.getName() + " is requesting to VC", vcrq.note },
                                    "VC Request Incoming",
                                    JOptionPane.YES_NO_OPTION,
                                    JOptionPane.INFORMATION_MESSAGE,
                                    null,
                                    new String[] { "Accept", "Decline" },
                                    null
                            );
                            if (result == 1) {
                                // decline
                                peer.send(new ProtoPacket(peer.nextPacketId(), Arrays.asList(new VCRejectMessage(peer.nextMessageId(), vcrq.message_id, "Declined by user"))));
                            } else if (result == 0) {
                                // accept
                                final int channel_id = new Random().nextInt();
                                postconnect(peer, channel_id);
                                peer.send(new ProtoPacket(peer.nextPacketId(), Arrays.asList(new VCAcceptMessage(peer.nextMessageId(), channel_id, vcrq.message_id))));
                            }
                        });
                    } else if (m instanceof VCDisconnectMessage vcdm) {
                        // the remote wishes to disconnect
                        if (cm != null && cm.peer == peer) {
                            cm.mode = ConnectionMode.Mode.Disconnected;
                            Main.window.connectionUpdate();
                            AudioManager.setActiveInputConsumer(null);
                        }
                    }
                }
            } else if (nvp instanceof DataPacket dp) {
                if (cm != null && dp.channel_id == cm.channel_id && cm.mode == ConnectionMode.Mode.Connected) {
                    // pipe this packet!
                    StreamOutputDriver drv = AudioManager.getOutputDriver();
                    if (drv != null)
                        drv.dispatch(dp.data.array());
                } else {
                    // silently discard the packet
                }
            }
            return false;
        });
    }
    public static void sendEcho(PeerInfo peer) {
        long key = new Random().nextLong();
        pending_echoes.put(key, System.currentTimeMillis());
        NetworkManager.sendPacket(new EchoPacket(peer.nextPacketId(), key), peer.remote);
    }
    public static void connectVC(PeerInfo peer, String note) {
        final ConnectionMode cm = new ConnectionMode();
        cm.peer = peer;
        cm.mode = NetworkManager.ConnectionMode.Mode.Connecting;
        final VCRequestMessage rqmsg = new VCRequestMessage(cm.request_id = peer.nextMessageId(), note);
        NetworkManager.connectionMode = cm;
        Main.window.connectionUpdate();
        NetworkManager.addTask(() -> {
            ProtoPacket p = new ProtoPacket(peer.nextPacketId(), Arrays.asList(rqmsg));
            NetworkManager.sendPacket(p, peer.remote);
        });
        NetworkManager.addPacketHook((packet, peer2) -> {
            if (peer2 != peer) return false;
            if (!(packet instanceof ProtoPacket)) return false;
            for (Message m : ((ProtoPacket) packet).messages) {
                if (m instanceof VCRequestAcknowledgeMessage vcram) {
                    if (vcram.request_id == cm.request_id) {
                        if (cm.mode == ConnectionMode.Mode.Connecting) {
                            cm.mode = ConnectionMode.Mode.Waiting;
                            Main.window.connectionUpdate();
                        }
                        return true;
                    }
                }
            }
            return false;
        });
        NetworkManager.addPacketHook((packet, peer2) -> {
            if (peer2 != peer) return false;
            if (!(packet instanceof ProtoPacket)) return false;
            for (Message m : ((ProtoPacket) packet).messages) {
                if (m instanceof VCAcceptMessage vcam) {
                    if (vcam.request_id == cm.request_id) {
                        postconnect(peer, vcam.remote_channel);
                        return true;
                    }
                } else if (m instanceof VCRejectMessage vcrm) {
                    if (vcrm.request_id == cm.request_id) {
                        cm.mode = ConnectionMode.Mode.Rejected;
                        Main.window.connectionUpdate();
                        Main.window.tasks.add(() -> {
                            JOptionPane.showMessageDialog(Main.window, new String[] { "Your request to VC was denied.", vcrm.note }, "VC Request Denied", JOptionPane.ERROR_MESSAGE);
                        });
                        return true;
                    }
                }
            }
            return false;
        });
    }
    private static void postconnect(PeerInfo peer, int channel_id) {
        ConnectionMode cm = new ConnectionMode();
        cm.mode = ConnectionMode.Mode.Connected;
        cm.peer = peer;
        cm.channel_id = channel_id;
        NetworkManager.connectionMode = cm;
        Main.window.connectionUpdate();
        AudioManager.setActiveInputConsumer(data -> {
            NetworkManager.sendPacket(new DataPacket(peer.nextPacketId(), channel_id, cm.sequence++, data), peer.remote);
        });
        AudioManager.getOutputDriver().silenced = false;
        peer.last_connect_time = System.currentTimeMillis();
        Main.window.peersUpdate();
    }
    public static void sendDowngradeRequest(PeerInfo peer, boolean immediate) {
        if (!immediate && (!downgrade_request_times.containsKey(peer.runtime_id) || downgrade_request_times.get(peer.runtime_id) + 5000 < System.currentTimeMillis())) return;
        downgrade_request_times.put(peer.runtime_id, System.currentTimeMillis());
        peer.send(new ProtoPacket(peer.nextPacketId(), Arrays.asList(new ProtoDowngradeMessage(peer.nextMessageId()))));
    }
    public static void disconnect() {
        if (NetworkManager.connectionMode == null) return;
        if (NetworkManager.connectionMode instanceof ConnectionMode cm) {
            if (!cm.mode.finalized) {
                NetworkManager.sendPacket(new ProtoPacket(cm.peer.nextPacketId(), Arrays.asList(new VCDisconnectMessage(cm.peer.nextMessageId()))), cm.peer.remote);
            }
            AudioManager.setActiveInputConsumer(null);
            AudioManager.getOutputDriver().silenced = true;
            NetworkManager.connectionMode = null;
            Main.window.connectionUpdate();
            cm.peer.last_connect_time = System.currentTimeMillis();
        }
    }
    private static class ConnectionMode extends NetworkManager.ConnectionMode {
        public short request_id;
        public int channel_id;
        public int sequence = 1;
    }
}
