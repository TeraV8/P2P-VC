package io.terav.vc.net;

import io.terav.vc.NetworkManager;
import java.net.InetAddress;

@SuppressWarnings("EqualsAndHashcode")
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
    /** Timestamp of last outbound packet */
    public long last_packet_time = -1;
    /** Timestamp of last packet received */
    public long last_receipt_time = -1;
    /** Timestamp of last connection */
    public long last_connect_time = -1;
    
    private long modhash;
    
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
        NetworkManager.sendPacket(p, remote);
        last_packet_time = System.currentTimeMillis();
    }
    
    private long getHash() {
        long hash = remote.hashCode() & 0xFFFF_FFFFL;
        if (hostname != null) hash ^= hostname.hashCode() & 0xFFFF_FFFFL;
        if (nickname != null) hash ^= nickname.hashCode() & 0xFFFF_FFFFL;
        hash <<= 8;
        hash ^= protover_hi & 0xFFFFL;
        hash <<= 16;
        hash ^= protover_compat & 0xFFFFL;
        hash <<= 8;
        hash ^= last_receipt_time;
        hash = (hash >>> 24) | ((hash & 0xFFFFFFL) << 40);
        hash ^= last_connect_time;
        return hash;
    }
    public boolean modified() {
        return getHash() != modhash;
    }
    public synchronized void clearModified() {
        modhash = getHash();
    }
    @Override
    public int hashCode() {
        return Long.hashCode(runtime_id);
    }
}
