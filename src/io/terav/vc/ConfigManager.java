package io.terav.vc;

import io.terav.vc.net.PeerInfo;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Properties;

public final class ConfigManager {
    private static final Properties master = new Properties();
    private static final HashMap<Long, String> fileNames = new HashMap<>();
    private ConfigManager() {}
    
    public static File getDataDir() {
        File dataDir = Paths.get(System.getProperty("user.home"), "AppData", "Roaming", "P2P-VC").toFile();
        if (!dataDir.getParentFile().exists())
            dataDir = Paths.get(System.getProperty("user.home"), ".local", "share", "P2P-VC").toFile();
        if (!dataDir.getParentFile().exists())
            dataDir = new File(System.getProperty("user.home", ".p2p-vc"));
        return dataDir;
    }
    public static void setupConfig() {
        final File dataDir = getDataDir();
        final File peersDir = new File(dataDir, "peers");
        peersDir.mkdirs();
        final File rootCfg = new File(dataDir, "app.cfg");
        try {
            rootCfg.createNewFile();
        } catch (IOException ioe) {
            System.err.println("Failed to create app.cfg: " + ioe.toString());
        }
    }
    public static boolean loadConfig() {
        final File dataDir = getDataDir();
        if (!dataDir.exists()) return false;
        final File rootCfg = new File(dataDir, "app.cfg");
        if (!rootCfg.exists()) return false;
        final File peersDir = new File(dataDir, "peers");
        if (!peersDir.exists()) peersDir.mkdir();
        
        try (FileInputStream fin = new FileInputStream(rootCfg)) {
            master.load(fin);
        } catch (IOException ioe) {
            System.err.println("Failed to load app.cfg: " + ioe.toString());
            System.exit(1);
        }
        // load config defaults here
        getBooleanProperty("input.mute", false);
        getDoubleProperty("output.volume", 1.0);
        
        File[] peerFiles = peersDir.listFiles();
        for (File peerFile : peerFiles) {
            try (FileInputStream fin = new FileInputStream(peerFile)) {
                Properties p = new Properties();
                p.load(fin);
                InetAddress addr = InetAddress.getByName(p.getProperty("address"));
                PeerInfo peer = new PeerInfo(addr);
                peer.hostname = p.getProperty("hostname");
                peer.nickname = p.getProperty("nickname");
                try {
                    peer.last_connect_time = Long.parseLong(p.getProperty("last_connect", "-1"));
                } catch (NumberFormatException e) {}
                try {
                    peer.last_receipt_time = Long.parseLong(p.getProperty("last_receipt", "-1"));
                } catch (NumberFormatException e) {}
                try {
                    peer.protover_hi = Short.parseShort(p.getProperty("protover_hi", "-1"));
                } catch (NumberFormatException e) {}
                try {
                    peer.protover_compat = Short.parseShort(p.getProperty("protover_compat", "-1"));
                } catch (NumberFormatException e) {}
                fileNames.put(peer.runtime_id, peerFile.getName());
                peer.clearModified();
                NetworkManager.registerPeer(peer, true);
            } catch (UnknownHostException e) {
                System.err.println("Peer file " + peerFile.getName() + " contained invalid address");
            } catch (IOException ioe) {
                System.err.println("Failed to read peer file " + peerFile.getName() + ": " + ioe.toString());
            }
        }
        
        Thread shutdown = new Thread(() -> {
            try (FileOutputStream fout = new FileOutputStream(rootCfg)) {
                master.store(fout, null);
            } catch (IOException ioe) {
                System.err.println("Failed to save app.cfg: " + ioe.toString());
            }
            for (PeerInfo peer : NetworkManager.getPeers()) {
                if (peer.modified()) {
                    System.out.println(peer.getName() + " was modified");
                    File peerFile = new File(peersDir, fileNames.getOrDefault(peer.runtime_id, peer.runtime_id + ".cfg"));
                    try (FileOutputStream fout = new FileOutputStream(peerFile)) {
                        Properties p = new Properties();
                        p.setProperty("address", peer.remote.getHostAddress());
                        if (peer.hostname != null)
                            p.setProperty("hostname", peer.hostname);
                        if (peer.nickname != null)
                            p.setProperty("nickname", peer.nickname);
                        if (peer.last_connect_time != -1L)
                            p.setProperty("last_connect", "" + peer.last_connect_time);
                        if (peer.last_receipt_time != -1L)
                            p.setProperty("last_receipt", "" + peer.last_receipt_time);
                        if (peer.protover_hi != -1)
                            p.setProperty("protover_hi", "" + peer.protover_hi);
                        if (peer.protover_compat != -1)
                            p.setProperty("protover_compat", "" + peer.protover_compat);
                        p.store(fout, null);
                    } catch (IOException ioe) {
                        System.err.println("Failed to save peer information for " + peer.getName() + ": " + ioe.toString());
                    }
                }
            }
        });
        shutdown.setName("SaveConfig");
        Runtime.getRuntime().addShutdownHook(shutdown);
        
        return true;
    }
    static void deletePeerConfig(PeerInfo peer) {
        File file = getPeerInfoFile(peer);
        if (file != null && file.exists()) file.delete();
    }
    private static File getPeerInfoFile(PeerInfo peer) {
        final File dataDir = getDataDir();
        if (!dataDir.exists()) return null;
        final File rootCfg = new File(dataDir, "app.cfg");
        if (!rootCfg.exists()) return null;
        final File peersDir = new File(dataDir, "peers");
        if (!peersDir.exists()) return null;
        String filename = fileNames.get(peer.runtime_id);
        if (filename == null) return null;
        return new File(peersDir, filename);
    }
    public static String getStringProperty(String prop, String def) {
        String ret = master.getProperty(prop);
        if (ret == null) {
            master.setProperty(prop, def);
            return def;
        }
        return ret;
    }
    public static void setStringProperty(String prop, String val) {
        master.setProperty(prop, val);
    }
    public static boolean getBooleanProperty(String prop, boolean def) {
        String ret = master.getProperty(prop);
        if (ret == null) {
            master.setProperty(prop, "" + def);
            return def;
        }
        ret = ret.trim();
        if (ret.equalsIgnoreCase("true") || ret.equals("1") || ret.equalsIgnoreCase("yes") || ret.equalsIgnoreCase("on"))
            return true;
        if (ret.equalsIgnoreCase("false") || ret.equals("0") || ret.equalsIgnoreCase("no") || ret.equalsIgnoreCase("off"))
            return false;
        master.setProperty(prop, "" + def);
        return def;
    }
    public static void setBooleanProperty(String prop, boolean val) {
        master.setProperty(prop, "" + val);
    }
    public static boolean toggleBooleanProperty(String prop, boolean idef) {
        boolean val = getBooleanProperty(prop, idef);
        setBooleanProperty(prop, !val);
        return !val;
    }
    public static double getDoubleProperty(String prop, double def) {
        String ret = master.getProperty(prop);
        if (ret == null) {
            master.setProperty(prop, "" + def);
            return def;
        }
        ret = ret.trim();
        try {
            return Double.parseDouble(ret);
        } catch (NumberFormatException e) {}
        master.setProperty(prop, "" + def);
        return def;
    }
    public static void setDoubleProperty(String prop, double val) {
        master.setProperty(prop, "" + val);
    }
    public static InetAddress getAddressProperty(String prop) {
        String ret = master.getProperty(prop);
        if (ret == null)
            return null;
        ret = ret.trim();
        try {
            return InetAddress.getByName(ret);
        } catch (UnknownHostException e) {}
        return null;
    }
}
