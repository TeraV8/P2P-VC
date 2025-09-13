package io.terav.vc;

import io.terav.vc.net.PeerInfo;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

final class ConnectDialog extends JDialog {
    private static ConnectDialog dialog = null;
    
    private JTextField address;
    private JTextArea note;
    private JLabel status;

    private ConnectDialog() {
        super(Main.window, "Connect to peer", true);
        setSize(300, 200);
        setResizable(false);
        setLocationRelativeTo(Main.window);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.gridheight = 1;
        gbc.weightx = 1.0;

        add(new JLabel("Enter connection address and (optional) note"), gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(address = new JTextField(), gbc);
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        JScrollPane jsp = new JScrollPane(note = new JTextArea(), JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        note.setLineWrap(true);
        note.setWrapStyleWord(true);
        add(jsp, gbc);
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.VERTICAL;
        add(status = new JLabel(" "), gbc);

        JButton connect = new JButton("Connect");
        connect.addActionListener(e -> {
            address.setBackground(Color.white);
            status.setText(" ");
            try {
                InetAddress addr = InetAddress.getByName(address.getText());
                if (addr.isAnyLocalAddress() || addr.isMulticastAddress()) {
                    address.setBackground(Color.pink);
                    status.setText("Cannot connect to this address");
                    //return;
                }
                if (note.getText().length() > 250) {
                    JOptionPane.showMessageDialog(this, "Message is too long!", "Message too long", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                setVisible(false);
                NetworkManager.connectVC(addr, note.getText());
            } catch (UnknownHostException ex) {
                address.setBackground(Color.pink);
                status.setText("Invalid IP address");
            }
        });
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> {
            setVisible(false);
        });
        gbc.weightx = 1.0;
        gbc.gridwidth = 1;
        add(connect, gbc);
        gbc.weightx = 0.0;
        add(Box.createHorizontalStrut(10), gbc);
        gbc.weightx = 1.0;
        add(cancel, gbc);
    }
    static synchronized void prompt(String addr) {
        ConnectDialog use = dialog;
        if (dialog == null)
            dialog = use = new ConnectDialog();
        
        final PeerInfo connected = NetworkManager.getConnectedPeer();
        if (connected != null) {
            int opt = JOptionPane.showConfirmDialog(Main.window, "Disconnect from " + connected.getName() + "?", "Confirm disconnect", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if (opt == JOptionPane.CANCEL_OPTION) return;
            NetworkManager.disconnectVC();
        }
        use.address.setText(addr);
        use.note.setText("");
        use.status.setText(" ");
        use.address.setBackground(Color.white);
        use.setVisible(true);
        if (use.address.getText().isEmpty())
            use.address.requestFocus();
        else
            use.note.requestFocus();
    }
    @Override
    public Insets getInsets() {
        final Insets sup = super.getInsets();
        return new Insets(sup.top + 10, sup.left + 10, sup.right + 10, sup.bottom + 10);
    }
}
