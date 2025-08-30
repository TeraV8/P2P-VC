package io.terav.vc;

import java.io.File;
import javax.swing.JOptionPane;

public class Main {
    public static final String VERSION = "0.0-0-alpha";
    public static AppWindow window;
    public static NetworkManager netman;
    public static void main(String[] args) {
        File dataDir = ConfigManager.getDataDir();
        
        netman = NetworkManager.start();
        netman.thread.start();
        
        if (!ConfigManager.loadConfig()) {
            // setup
            int opt = JOptionPane.showConfirmDialog(null, "P2P-VC will create its configuration at\n" + dataDir.getAbsolutePath(), "Confirm Setup", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
            if (opt == JOptionPane.CANCEL_OPTION) return;
            ConfigManager.setupConfig();
            ConfigManager.loadConfig();
        }
        
        AudioManager.start();
        window = new AppWindow();
        
        window.setVisible(true);
    }
}
