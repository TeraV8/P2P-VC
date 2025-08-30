package io.terav.vc.net;

import io.terav.vc.Main;
import java.net.InetAddress;

public final class PeerInfo {
    /** Runtime-constant identifier for this object */
    public final long runtime_id = System.currentTimeMillis() ^ System.nanoTime();
    /** Known remote address of this peer */
    public InetAddress remote;
    /** Hostname (can be obtained from remote) */
    public String hostname = null;
    /** Nickname (user-assigned) */
    public String nickname = null;
    /** Known supported high protocol version */
    public short protover_hi = -1;
    /** Selected protocol version to use */
    public short protover_compat = -1;
    public int last_packet_id = 0;
    public short last_message_id = 0;
    /** Timestamp of last outboud packet */
    public long last_packet_time = -1;
    /** Timestamp of last packet received */
    public long last_receipt_time = -1;
    /** Timestamp of last connection */
    public long last_connect_time = -1;
    
    public PeerInfo(InetAddress addr) {
        this.remote = addr;
    }
    
    public String getName() {
        if (nickname != null) return nickname;
        if (hostname != null) return '[' + hostname + ']';
        return '<' + remote.getHostAddress() + '>';
    }
    public synchronized int nextPacketId() {
        return ++last_packet_id;
    }
    public synchronized short nextMessageId() {
        return ++last_message_id;
    }
    public void send(Packet p) {
        Main.netman.sendPacket(p, remote);
        last_packet_time = System.currentTimeMillis();
    }
    
    @Override
    public int hashCode() {
        int hash = remote.hashCode();
        if (hostname != null) hash ^= hostname.hashCode();
        if (nickname != null) hash ^= nickname.hashCode();
        hash ^= protover_hi << 16;
        hash ^= protover_compat & 0xFFFF;
        hash ^= Long.hashCode(last_receipt_time);
        hash ^= Long.hashCode(last_connect_time);
        return hash;
    }
}
