package io.terav.vc;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Insets;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

final class AppInfoDialog extends JDialog {
    private static AppInfoDialog dialog = null;
    
    private AppInfoDialog() {
        super(Main.window, "About P2P-VC", false);
        setSize(350, 220);
        setResizable(false);
        setLayout(new BorderLayout());
        JLabel contents = new JLabel();
        contents.setFont(contents.getFont().deriveFont(Font.PLAIN));
        contents.setText("<html><h1>P2P-VC</h1><p>Version " + Main.VERSION + "<br>Open-source peer-to-peer voice communication<br>Created by TeraV<br>Using GNU General Public License v3<br><a href>https://github.com/TeraV8/P2P-VC</a></p>");
        add(contents, BorderLayout.CENTER);
        JPanel buttonPane = new JPanel();
        JButton github = new JButton("GitHub Repo");
        github.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new URI("https://github.com/TeraV8/P2P-VC"));
            } catch (IOException | URISyntaxException ex) {}
        });
        JButton update = new JButton("Check for updates");
        update.addActionListener(e -> {
            update.setEnabled(false);
            try {
                HttpURLConnection conn = (HttpURLConnection) URI.create("https://terav8.github.io/P2P-VC/version").toURL().openConnection();
                if (conn.getResponseCode() != 200)
                    throw new RuntimeException("HTTP response " + conn.getResponseCode());
                byte[] data = new byte[conn.getContentLength()];
                try (InputStream in = (InputStream) conn.getContent()) {
                    in.read(data);
                }
                String[] newver = new String(data).trim().split("[\\.-]");
                String[] oldver = Main.VERSION.split("[\\.-]");
                boolean updates = false;
                for (int i = 0; i < oldver.length; i++) {
                    if (i >= newver.length) break;
                    if (newver[i].compareTo(oldver[i]) > 0) {
                        try {
                            Desktop.getDesktop().browse(new URI("https://github.com/TeraV8/P2P-VC/releases/tag/v" + new String(data).trim()));
                        } catch (IOException | URISyntaxException ex) {}
                        updates = true;
                        break;
                    }
                }
                if (!updates)
                    JOptionPane.showMessageDialog(AppInfoDialog.this, "P2P-VC is up to date!", "Up to date", JOptionPane.INFORMATION_MESSAGE);
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
    static synchronized void open() {
        AppInfoDialog d = dialog;
        if (dialog == null) dialog = d = new AppInfoDialog();
        
        d.setLocationRelativeTo(Main.window);
        d.setVisible(true);
        d.requestFocus();
    }
    @Override
    public Insets getInsets() {
        final Insets sup = super.getInsets();
        return new Insets(sup.top + 0, sup.left + 10, sup.right + 10, sup.bottom + 10);
    }
}
