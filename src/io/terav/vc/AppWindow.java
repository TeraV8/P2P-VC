package io.terav.vc;

import io.terav.vc.net.PeerInfo;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.time.Duration;
import java.time.Period;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import javax.sound.sampled.Mixer;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

public class AppWindow extends JFrame implements Runnable {
    private Icon PEER_OFFLINE;
    private Icon PEER_ONLINE;
    private Icon PEER_IDLE;
    
    private DirectConnectModal modal_dconn;
    private AppInfoDialog dialog_appinfo;
    private PeerInfoDialog dialog_peerinfo;
    private final JPanel statusBar;
    private final JMenuItem item_discon;
    private final JLabel conn_label;
    private final JButton conn_discon;
    private final JList channels;
    private ArrayList<PeerInfo> peerlist = new ArrayList<>();
    private final Thread thread;
    public final LinkedBlockingQueue<Runnable> tasks = new LinkedBlockingQueue<>();
    
    AppWindow() {
        super("P2P-VC " + Main.VERSION);
        
        PEER_ONLINE = new ImageIcon(ClassLoader.getSystemResource("online.png"));
        PEER_OFFLINE = new ImageIcon(ClassLoader.getSystemResource("offline.png"));
        PEER_IDLE = new ImageIcon(ClassLoader.getSystemResource("idle.png"));
        
        this.modal_dconn = new DirectConnectModal();
        this.dialog_appinfo = new AppInfoDialog();
        this.dialog_peerinfo = new PeerInfoDialog();
        this.thread = new Thread(this);
        this.thread.setName("UIUtilThread");
        this.thread.setDaemon(true);
        this.thread.start();
        
        setSize(640, 480);
        setMinimumSize(new Dimension(320, 240));
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) { appControlExit(); }
        });
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        
        JButton conn_mute = new JButton("Mute");
        JMenuBar menubar = new JMenuBar();
            JMenu menu_app = new JMenu("Application");
                JMenuItem item_exit = new JMenuItem("Exit");
                item_exit.addActionListener(e -> appControlExit());
                menu_app.add(item_exit);
            menubar.add(menu_app);
            JMenu menu_aud = new JMenu("Audio");
                JCheckBoxMenuItem item_mute = new JCheckBoxMenuItem("Mute microphone");
                item_mute.setState(ConfigManager.getBooleanProperty("input.mute", false));
                item_mute.addChangeListener(e -> {
                    ConfigManager.setBooleanProperty("input.mute", item_mute.getState());
                    conn_mute.setText(item_mute.getState() ? "Unmute" : "Mute");
                });
                menu_aud.add(item_mute);
                JMenu menu_adev = new JMenu("Audio devices");
                    JMenuItem item_inphead = new JMenuItem("Input devices");
                    item_inphead.setEnabled(false);
                    menu_adev.add(item_inphead);
                    ButtonGroup group_inpsel = new ButtonGroup();
                    for (Mixer inpdev : AudioManager.inputDevices) {
                        JRadioButtonMenuItem item_inpdev = new JRadioButtonMenuItem(inpdev.getMixerInfo().getName());
                        item_inpdev.addActionListener(e -> AudioManager.setActiveInput(inpdev));
                        menu_adev.addMenuListener(new MenuListener() {
                            @Override
                            public void menuSelected(MenuEvent e) {
                                item_inpdev.setSelected(AudioManager.activeInput == inpdev);
                            }
                            @Override
                            public void menuDeselected(MenuEvent e) {}
                            @Override
                            public void menuCanceled(MenuEvent e) {}
                        });
                        group_inpsel.add(item_inpdev);
                        menu_adev.add(item_inpdev);
                    }
                    if (AudioManager.inputDevices.isEmpty()) item_inphead.setText("No input devices found");
                    JMenuItem item_outhead = new JMenuItem("Output devices");
                    item_outhead.setEnabled(false);
                    menu_adev.add(item_outhead);
                    ButtonGroup group_outsel = new ButtonGroup();
                    for (Mixer outdev : AudioManager.outputDevices) {
                        JRadioButtonMenuItem item_outdev = new JRadioButtonMenuItem(outdev.getMixerInfo().getName());
                        item_outdev.addActionListener(e -> AudioManager.setActiveOutput(outdev));
                        menu_adev.addMenuListener(new MenuListener() {
                            @Override
                            public void menuSelected(MenuEvent e) {
                                item_outdev.setSelected(AudioManager.activeOutput == outdev);
                            }
                            @Override
                            public void menuDeselected(MenuEvent e) {}
                            @Override
                            public void menuCanceled(MenuEvent e) {}
                        });
                        group_outsel.add(item_outdev);
                        menu_adev.add(item_outdev);
                    }
                    if (AudioManager.outputDevices.isEmpty()) item_outhead.setText("No output devices found");
                    menu_adev.add(new JSeparator());
                    JMenuItem item_devmgmt = new JMenuItem("Audio devices...");
                    item_devmgmt.setEnabled(false);
                    item_devmgmt.addActionListener(e -> {}); // TODO
                    menu_adev.add(item_devmgmt);
                menu_aud.add(menu_adev);
            menubar.add(menu_aud);
            JMenu menu_net = new JMenu("Network");
                JMenuItem item_dconn = new JMenuItem("Connect to peer...");
                item_dconn.addActionListener(e -> {
                    modal_dconn.prompt("");
                });
                menu_net.add(item_dconn);
                item_discon = new JMenuItem("Disconnect");
                item_discon.setEnabled(false);
                item_discon.addActionListener(e -> {
                    Main.netman.disconnectVC();
                });
                menu_net.add(item_discon);
                menu_net.addSeparator();
                JMenuItem item_showip = new JMenuItem("Show IP address");
                item_showip.addActionListener(e -> {
                    // TODO
                    InetAddress addr = null;
                    try {
                        addr = InetAddress.getLocalHost();
                        if (!(addr instanceof Inet6Address)) {
                            NetworkInterface inf = NetworkInterface.getByInetAddress(addr);
                            for (InetAddress ia : inf.inetAddresses().toList()) {
                                if (ia instanceof Inet6Address && !ia.isLinkLocalAddress()) {
                                    addr = ia;
                                    break;
                                }
                            }
                        }
                    } catch (UnknownHostException | SocketException ex) {}
                    if (addr instanceof Inet6Address) {
                        String address;
                        try {
                            address = InetAddress.getByAddress(addr.getAddress()).getHostAddress();
                        } catch (UnknownHostException ex) { return; } // this will never happen
                        if (JOptionPane.showOptionDialog(AppWindow.this, address, "IP Address", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE, null, new Object[] { "Copy", "OK" }, null) == JOptionPane.OK_OPTION) {
                            try {
                                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(address), null);
                            } catch (IllegalStateException ex) {}
                        }
                        return;
                    }
                    try {
                        Desktop.getDesktop().browse(new URI("https://www.showmyip.com/"));
                    } catch (URISyntaxException | IOException ex) {}
                });
                menu_net.add(item_showip);
            menubar.add(menu_net);
            JMenu menu_hlp = new JMenu("Help");
                JMenuItem item_about = new JMenuItem("About P2P-VC...");
                item_about.addActionListener(e -> {
                    dialog_appinfo.open();
                });
                menu_hlp.add(item_about);
            menubar.add(menu_hlp);
        setJMenuBar(menubar);
        
        JPanel connection = new JPanel() {
            @Override
            public Insets getInsets() {
                Insets i = super.getInsets();
                return new Insets(i.top + 10, i.left + 10, i.bottom + 10, i.right + 10);
            }
            @Override
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(Color.gray);
                g.fillRect(0, getHeight() - 1, getWidth(), 1);
            }
        };
        connection.setPreferredSize(new Dimension(-1, 60));
        //connection.setLayout(new GridLayout(1, 6, 10, 10));
        connection.setLayout(new BoxLayout(connection, BoxLayout.X_AXIS));
            conn_label = new JLabel("Not connected");
            connection.add(conn_label);
            connection.add(Box.createHorizontalGlue());
            if (ConfigManager.getBooleanProperty("input.mute", false))
                conn_mute.setText("Unmute");
            conn_mute.setMargin(new Insets(0, 0, 0, 0));
            conn_mute.setPreferredSize(new Dimension(80, 40));
            conn_mute.setMinimumSize(new Dimension(80, 40));
            conn_mute.setMaximumSize(new Dimension(80, 40));
            conn_mute.addActionListener(e -> {
                ConfigManager.toggleBooleanProperty("input.mute", false);
                item_mute.setState(ConfigManager.getBooleanProperty("input.mute", false));
                conn_mute.setText(ConfigManager.getBooleanProperty("input.mute", false) ? "Unmute" : "Mute");
            });
            connection.add(conn_mute);
            connection.add(Box.createHorizontalStrut(10));
            conn_discon = new JButton("Disconnect");
            conn_discon.setMargin(new Insets(0, 0, 0, 0));
            conn_discon.setPreferredSize(new Dimension(80, 40));
            conn_discon.setMinimumSize(new Dimension(80, 40));
            conn_discon.setMaximumSize(new Dimension(80, 40));
            conn_discon.setEnabled(false);
            conn_discon.addActionListener(e -> {
                Main.netman.disconnectVC();
            });
            connection.add(conn_discon);
        add(connection, BorderLayout.NORTH);
        channels = new JList(new DefaultListModel());
        channels.setPreferredSize(new Dimension(150, -1));
        channels.setLayoutOrientation(JList.VERTICAL);
        channels.setFixedCellWidth(150);
        channels.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        final DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();
        channels.setCellRenderer((JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) -> {
            PeerInfo peer = ((PeerInfo) value);
            JLabel comp = (JLabel) defaultRenderer.getListCellRendererComponent(list, peer.getName(), index, isSelected, cellHasFocus);
            if (System.currentTimeMillis() - peer.last_receipt_time > 60_000)
                comp.setIcon(PEER_OFFLINE);
            else
                comp.setIcon(PEER_ONLINE);
            return comp;
        });
        channels.addListSelectionListener(e -> {
            // TODO open text channel
        });
        JPopupMenu peer_ctx = new JPopupMenu();
            JMenuItem ctx_nick = new JMenuItem("Change nickname");
            ctx_nick.addActionListener(e -> {
                final PeerInfo peer = peerlist.get(channels.getSelectedIndex());
                String nick = (String) JOptionPane.showInputDialog(AppWindow.this, "Enter a new nickname for " + peer.getName() + " (" + peer.remote + ')', "Change peer nickname", JOptionPane.PLAIN_MESSAGE, null, null, peer.nickname);
                if (nick == null) return;
                if (nick.isBlank())
                    JOptionPane.showMessageDialog(AppWindow.this, "Chosen nickname must not be blank", "Invalid nickname", JOptionPane.ERROR_MESSAGE);
                else if (!nick.matches("^[^<>\\[\\]():*/\\\\]*$"))
                    JOptionPane.showMessageDialog(AppWindow.this, "Chosen nickname contains invalid characters", "Invalid nickname", JOptionPane.ERROR_MESSAGE);
                else
                    peer.nickname = nick;
                peersUpdate();
            });
            peer_ctx.add(ctx_nick);
            JMenuItem ctx_vcc = new JMenuItem("Connect");
            ctx_vcc.addActionListener(e -> {
                final PeerInfo peer = peerlist.get(channels.getSelectedIndex());
                if (peer == Main.netman.getConnectedPeer())
                    Main.netman.disconnectVC();
                else
                    modal_dconn.prompt(peer.remote.getHostAddress());
            });
            peer_ctx.add(ctx_vcc);
            JMenuItem ctx_info = new JMenuItem("Peer info");
            ctx_info.addActionListener(e -> {
                dialog_peerinfo.show(peerlist.get(channels.getSelectedIndex()));
            });
            peer_ctx.add(ctx_info);
        channels.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int selection = channels.getSelectedIndex();
                Rectangle bounds = channels.getCellBounds(selection, selection);
                if (bounds != null && bounds.contains(e.getPoint())) {
                    final PeerInfo peer = peerlist.get(selection);
                    if (e.getButton() == 3) {
                        ctx_vcc.setText((peer == Main.netman.getConnectedPeer()) ? "Disconnect" : "Connect");
                        peer_ctx.show(channels, e.getX(), e.getY());
                    } else if (e.getClickCount() == 2 && e.getButton() == 1) {
                        if (peer != Main.netman.getConnectedPeer())
                            modal_dconn.prompt(peer.remote.getHostAddress());
                    }
                }
            }
        });
        add(new JScrollPane(channels, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.WEST);
        peersUpdate();
        JPanel content = new JPanel();
        add(content, BorderLayout.CENTER);
        statusBar = new JPanel() {
            @Override
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D ctx = (Graphics2D) g;
                ctx.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
                ctx.setColor(Color.gray);
                ctx.fillRect(0, 0, getWidth(), 1);
                ctx.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
                ctx.setColor(Color.black);
                final String name = (Main.netman.connectionMode == null) ? null : Main.netman.connectionMode.peer.getName();
                String status = (Main.netman.connectionMode == null) ? "Standby" : switch (Main.netman.connectionMode.mode) {
                    case Connecting -> "Connecting to " + name + "...";
                    case Failed -> "Failed to connect to " + name;
                    case Waiting -> "Waiting to connect to " + name;
                    case Connected -> "Connected to " + name;
                    default -> "" + Main.netman.connectionMode.mode;
                };
                ctx.drawString(status, getWidth() - 200, 13);
            }
        };
        statusBar.setPreferredSize(new Dimension(-1, 16));
        add(statusBar, BorderLayout.SOUTH);
    }
    public void peersUpdate() {
        peerlist = new ArrayList<>(Main.netman.getPeers());
        peerlist.sort((a,b) -> {
            if (a.last_connect_time != b.last_connect_time) return Long.signum(b.last_connect_time - a.last_connect_time);
            return a.getName().compareTo(b.getName());
        });
        DefaultListModel model = (DefaultListModel) channels.getModel();
        model.setSize(peerlist.size());
        for (int i = 0; i < peerlist.size(); i++)
            model.set(i, peerlist.get(i));
    }
    public void connectionUpdate() {
        final boolean connected = Main.netman.connectionMode != null && !Main.netman.connectionMode.mode.finalized;
        item_discon.setEnabled(connected);
        conn_discon.setEnabled(connected);
        if (connected) {
            conn_label.setText(switch (Main.netman.connectionMode.mode) {
                case Connecting -> "Connecting...";
                case Waiting -> "Waiting for accept...";
                case Connected -> "Connected to " + Main.netman.connectionMode.peer.getName();
                default -> "" + Main.netman.connectionMode.mode;
            });
        } else {
            conn_label.setText("Not connected");
        }
        tasks.add(new RepaintStatusBarTask());
    }
    private void appControlExit() {
        Main.netman.stop();
        System.exit(0);
    }

    @Override
    public void run() {
        while (true) {
            try {
                tasks.take().run();
            } catch (InterruptedException e) {}
        }
    }
    
    private final class PeerInfoDialog extends JDialog {
        private final JLabel address;
        private final JLabel nickname;
        private final JLabel conntime;
        private final JLabel lastonline;
        private final JLabel protover;
        private PeerInfo peer;
        private PeerInfoDialog() {
            super(AppWindow.this, "Peer information", false);
            setSize(400, 200);
            setResizable(false);
            setLayout(new GridLayout(0, 2, 10, 5));
            setLocationRelativeTo(AppWindow.this);
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
                modal_dconn.prompt(peer.remote.getHostAddress());
            });
            add(connect);
            JButton forget = new JButton("Forget");
            forget.addActionListener(e -> {
                int opt = JOptionPane.showConfirmDialog(PeerInfoDialog.this, "Are you sure you want to forget " + peer.getName(), "Confirm forget peer", JOptionPane.WARNING_MESSAGE, JOptionPane.YES_NO_OPTION);
                if (opt == JOptionPane.YES_OPTION) {
                    Main.netman.forgetPeer(peer);
                    setVisible(false);
                }
            });
            add(forget);
        }
        private void show(PeerInfo peer) {
            this.peer = peer;
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
        }
        @Override
        public Insets getInsets() {
            final Insets sup = super.getInsets();
            return new Insets(sup.top + 10, sup.left + 10, sup.right + 10, sup.bottom + 10);
        }
    }
    private final class AppInfoDialog extends JDialog {
        private JLabel contents;
        
        private AppInfoDialog() {
            super(AppWindow.this, "About P2P-VC", false);
            setSize(350, 200);
            setResizable(false);
            setLayout(new BorderLayout());
            contents = new JLabel();
            contents.setFont(contents.getFont().deriveFont(Font.PLAIN));
            contents.setText("<html><h1>P2P-VC</h1><p>Version " + Main.VERSION + "<br>Open-source peer-to-peer voice communication<br>Created by TeraV<br><a href>https://github.com/TeraV8/P2P-VC</a></p>");
            add(contents, BorderLayout.CENTER);
            JPanel buttonPane = new JPanel();
            JButton github = new JButton("GitHub Repo");
            github.addActionListener(e -> {
                try {
                    Desktop.getDesktop().browse(new URI("https://github.com/TeraV8/P2P-VC"));
                } catch (Exception ex) {}
            });
            JButton update = new JButton("Check for updates");
            update.addActionListener(e -> {
                update.setEnabled(false);
                try {
                    HttpURLConnection conn = (HttpURLConnection) URI.create("https://terav8.github.io/P2P-VC/version").toURL().openConnection();
                    if (conn.getResponseCode() != 200)
                        throw new RuntimeException("HTTP response " + conn.getResponseCode());
                } catch (Exception ex) {
                    System.err.println(ex);
                    JOptionPane.showMessageDialog(AppInfoDialog.this, "Failed to check for updates", "Update check failed", JOptionPane.ERROR_MESSAGE);
                }
                update.setEnabled(true);
            });
            buttonPane.add(github);
            buttonPane.add(update);
            add(buttonPane, BorderLayout.SOUTH);
        }
        private void open() {
            setLocationRelativeTo(AppWindow.this);
            setVisible(true);
            requestFocus();
        }
        @Override
        public Insets getInsets() {
            final Insets sup = super.getInsets();
            return new Insets(sup.top + 0, sup.left + 10, sup.right + 10, sup.bottom + 10);
        }
    }
    private final class RepaintStatusBarTask implements Runnable {
        @Override
        public void run() {
            statusBar.repaint();
        }
    }
    private class DirectConnectModal extends JDialog {
        private JTextField address;
        private JTextArea note;
        private JLabel status;
        
        private DirectConnectModal() {
            super(AppWindow.this, "Connect to peer", true);
            setSize(300, 200);
            setResizable(false);
            setLocationRelativeTo(AppWindow.this);
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
                //if (!address.getText().matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")) {
                //    address.setBackground(Color.pink);
                //    status.setText("Invalid IP address");
                //} else {
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
                        Main.netman.connectVC(addr, note.getText());
                    } catch (UnknownHostException ex) {
                        address.setBackground(Color.pink);
                        status.setText("Invalid IP address");
                    }
                //}
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
        private void prompt(String addr) {
            final PeerInfo connected = Main.netman.getConnectedPeer();
            if (connected != null) {
                int opt = JOptionPane.showConfirmDialog(AppWindow.this, "Disconnect from " + connected.getName() + "?", "Confirm disconnect", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
                if (opt == JOptionPane.CANCEL_OPTION) return;
                Main.netman.disconnectVC();
            }
            address.setText(addr);
            note.setText("");
            status.setText(" ");
            address.setBackground(Color.white);
            setVisible(true);
            address.requestFocus();
        }
        @Override
        public Insets getInsets() {
            final Insets sup = super.getInsets();
            return new Insets(sup.top + 10, sup.left + 10, sup.right + 10, sup.bottom + 10);
        }
    }
}
