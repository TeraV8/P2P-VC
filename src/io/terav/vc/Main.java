package io.terav.vc;

import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import javax.swing.JOptionPane;

public class Main {
    public static final String VERSION = "1.1";
    public static AppWindow window;
    public static void main(String[] args) {
        File dataDir = ConfigManager.getDataDir();
        
        if (!ConfigManager.loadConfig()) {
            // setup
            int opt = JOptionPane.showConfirmDialog(null, "P2P-VC will create its configuration at\n" + dataDir.getAbsolutePath(), "Confirm Setup", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
            if (opt == JOptionPane.CANCEL_OPTION) return;
            ConfigManager.setupConfig();
            ConfigManager.loadConfig();
        }
        
        try {
            NetworkManager.start();
        } catch (BindException e) {
            ProcessHandle other = ConfigManager.getDuplicateProcess();
            if (other == null) {
                JOptionPane.showMessageDialog(null, "Failed to launch P2P-VC!\n" + e, "P2P-VC Error", JOptionPane.ERROR_MESSAGE);
                System.exit(2);
            }
            // notify the other process
            try (DatagramSocket socket = new DatagramSocket(0, InetAddress.getLoopbackAddress())) {
                DatagramPacket packet = new DatagramPacket(new byte[] { 0, 0, 0, 0, -1, -1, 0, 0 }, 8);
                packet.setPort(31416);
                packet.setAddress(InetAddress.getLoopbackAddress());
                socket.send(packet);
            } catch (IOException ioe) {}
            return;
        }
        AudioManager.start();
        ConfigManager.createLock();
        window = new AppWindow();
        
        window.setVisible(true);
    }
    
    public static void applicationMessage(byte message) {
        switch (message) {
            case 0 -> {
                System.out.println("Launched new app -> sending window to foreground");
                window.setVisible(true);
                window.toFront();
                window.requestFocus();
            }
        }
    }
}
