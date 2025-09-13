package io.terav.vc;

import io.terav.vc.net.PeerInfo;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

final class PeerInfoDialog extends JDialog {
    private static final HashMap<PeerInfo, PeerInfoDialog> dialogs = new HashMap<>();
    
    private final JLabel address;
    private final JLabel nickname;
    private final JLabel conntime;
    private final JLabel lastonline;
    private final JLabel protover;
    private final PeerInfo peer;
    
    private PeerInfoDialog(PeerInfo peer) {
        super(Main.window, "Peer information", false);
        setSize(400, 200);
        setResizable(false);
        setLayout(new GridLayout(0, 2, 10, 5));
        setLocationRelativeTo(Main.window);
        
        this.peer = peer;
        
        JLabel label = new JLabel("IP address");
        label.setHorizontalTextPosition(JLabel.RIGHT);
        add(label);
        final Font font = label.getFont().deriveFont(Font.PLAIN);
        address = new JLabel();
        address.setFont(font);
        add(address);
        label = new JLabel("Local nickname");
        label.setHorizontalTextPosition(JLabel.RIGHT);
        add(label);
        nickname = new JLabel();
        nickname.setFont(font);
        add(nickname);
        label = new JLabel("Last connection time");
        label.setHorizontalTextPosition(JLabel.RIGHT);
        add(label);
        conntime = new JLabel();
        conntime.setFont(font);
        add(conntime);
        label = new JLabel("Last seen online");
        label.setHorizontalTextPosition(JLabel.RIGHT);
        add(label);
        lastonline = new JLabel();
        lastonline.setFont(font);
        add(lastonline);
        label = new JLabel("Known protocol");
        label.setHorizontalTextPosition(JLabel.RIGHT);
        add(label);
        protover = new JLabel();
        protover.setFont(font);
        add(protover);
        JButton connect = new JButton("Connect");
        connect.addActionListener(e -> {
            ConnectDialog.prompt(peer.remote.getHostAddress());
        });
        add(connect);
        JButton forget = new JButton("Forget");
        forget.addActionListener(e -> {
            int opt = JOptionPane.showConfirmDialog(PeerInfoDialog.this, "Are you sure you want to forget " + peer.getName(), "Confirm forget peer", JOptionPane.WARNING_MESSAGE, JOptionPane.YES_NO_OPTION);
            if (opt == JOptionPane.YES_OPTION) {
                NetworkManager.forgetPeer(peer);
                setVisible(false);
            }
        });
        add(forget);
    }
    private void showDialog() {
        address.setText(peer.remote.toString());
        nickname.setText(peer.nickname);
        conntime.setText((peer.last_connect_time == -1L) ? "never" : DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(new Date(peer.last_connect_time)));
        if (peer.last_receipt_time == -1)
            lastonline.setText("never");
        else if (System.currentTimeMillis() - peer.last_receipt_time < 120_000)
            lastonline.setText("Just now");
        else {
            int minutes = (int) ((System.currentTimeMillis() - peer.last_receipt_time) / 60_000);
            if (minutes < 60)
                lastonline.setText(minutes + " minutes ago");
            else if (minutes < 1440)
                lastonline.setText(minutes / 60 + " hours ago");
            else
                lastonline.setText(minutes / 1440 + " days ago");
        }
        protover.setText((peer.protover_hi == -1) ? "unknown" : "v" + peer.protover_hi);
        setVisible(true);
        requestFocus();
    }
    static synchronized void show(PeerInfo peer) {
        PeerInfoDialog dialog;
        dialog = dialogs.get(peer);
        if (dialog == null) dialogs.put(peer, dialog = new PeerInfoDialog(peer));
        dialog.showDialog();
        
    }
    @Override
    public Insets getInsets() {
        final Insets sup = super.getInsets();
        return new Insets(sup.top + 10, sup.left + 10, sup.right + 10, sup.bottom + 10);
    }
}
