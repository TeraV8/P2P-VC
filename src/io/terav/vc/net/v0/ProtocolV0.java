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
    // TODO stuf
    private ProtocolV0() {}
    public static void activateProtocolProcessor(NetworkManager nm) {
        nm.addPacketHook((nvp,peer) -> {
            ConnectionMode cm = null;
            if (Main.netman.connectionMode != null && (Main.netman.connectionMode instanceof ConnectionMode)) cm = (ConnectionMode) Main.netman.connectionMode;
            final ConnectionMode fcm = cm; // workaround for finicky fields in lambda expressions
            if (nvp instanceof EchoPacket p) {
                if (pending_echoes.containsKey(p.content))
                    pending_echoes.remove(p.content);
                else
                    nm.sendPacket(new EchoPacket(peer.nextMessageId(), p.content), peer.remote);
            } else if (nvp instanceof ProtoPacket p) {
                for (Message m : p.messages) {
                    if (m instanceof VCRequestMessage vcrq) {
                        // send an acknowledgement posthaste
                        nm.sendPacket(new ProtoPacket(peer.nextPacketId(), Arrays.asList(new VCRequestAcknowledgeMessage(peer.nextMessageId(), m.message_id))), peer.remote);
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
                                nm.sendPacket(new ProtoPacket(peer.nextPacketId(), Arrays.asList(new VCRejectMessage(peer.nextMessageId(), vcrq.message_id, "Declined by user"))), peer.remote);
                            } else if (result == 0) {
                                // accept
                                final int channel_id = new Random().nextInt();
                                postconnect(peer, channel_id);
                                nm.sendPacket(new ProtoPacket(peer.nextPacketId(), Arrays.asList(new VCAcceptMessage(peer.nextMessageId(), channel_id, vcrq.message_id))), peer.remote);
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
        Main.netman.sendPacket(new EchoPacket(peer.nextPacketId(), key), peer.remote);
    }
    public static void connectVC(PeerInfo peer, String note) {
        final ConnectionMode cm = new ConnectionMode();
        cm.peer = peer;
        cm.mode = NetworkManager.ConnectionMode.Mode.Connecting;
        final VCRequestMessage rqmsg = new VCRequestMessage(cm.request_id = peer.nextMessageId(), note);
        Main.netman.connectionMode = cm;
        Main.window.connectionUpdate();
        Main.netman.addTask(() -> {
            ProtoPacket p = new ProtoPacket(peer.nextPacketId(), Arrays.asList(rqmsg));
            Main.netman.sendPacket(p, peer.remote);
        });
        Main.netman.addPacketHook((packet, peer2) -> {
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
        Main.netman.addPacketHook((packet, peer2) -> {
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
        Main.netman.connectionMode = cm;
        Main.window.connectionUpdate();
        AudioManager.setActiveInputConsumer(data -> {
            Main.netman.sendPacket(new DataPacket(peer.nextPacketId(), channel_id, cm.sequence++, data), peer.remote);
        });
        AudioManager.getOutputDriver().silenced = false;
        peer.last_connect_time = System.currentTimeMillis();
    }
    public static void disconnect() {
        if (Main.netman.connectionMode == null) return;
        if (Main.netman.connectionMode instanceof ConnectionMode cm) {
            if (!cm.mode.finalized) {
                Main.netman.sendPacket(new ProtoPacket(cm.peer.nextPacketId(), Arrays.asList(new VCDisconnectMessage(cm.peer.nextMessageId()))), cm.peer.remote);
            }
            AudioManager.setActiveInputConsumer(null);
            AudioManager.getOutputDriver().silenced = true;
            Main.netman.connectionMode = null;
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
