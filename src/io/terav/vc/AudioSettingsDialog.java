package io.terav.vc;

import java.awt.GridLayout;
import java.awt.Insets;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Mixer;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JSlider;

final class AudioSettingsDialog extends JDialog {
    private static AudioSettingsDialog dialog = null;
    
    private boolean selectionLock = false;
    private final JComboBox opt_idev;
    private final JComboBox opt_odev;
    private final JSlider opt_ivol;
    private final JSlider opt_ovol;
    
    private AudioSettingsDialog() {
        super(Main.window, "Audio settings", false);
        setSize(350, 190);
        setResizable(false);
        setLocationRelativeTo(Main.window);
        setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        setLayout(new GridLayout(0, 2, 10, 10));
        
        add(fieldLabel("Input device"));
        opt_idev = new JComboBox();
        opt_idev.addItemListener(e -> {
            if (selectionLock || opt_idev.getSelectedIndex() == -1) return;
            AudioManager.setActiveInput(AudioManager.inputDevices.get(opt_idev.getSelectedIndex()));
        });
        add(opt_idev);
        add(fieldLabel("Output device"));
        opt_odev = new JComboBox();
        opt_odev.addItemListener(e -> {
            if (selectionLock || opt_odev.getSelectedIndex() == -1) return;
            AudioManager.setActiveOutput(AudioManager.outputDevices.get(opt_odev.getSelectedIndex()));
        });
        add(opt_odev);
        add(fieldLabel("Input volume"));
        opt_ivol = new JSlider(0, 200, 100);
        opt_ivol.setMajorTickSpacing(100);
        opt_ivol.setMinorTickSpacing(20);
        opt_ivol.setPaintTicks(true);
        opt_ivol.addChangeListener(e -> {
            FloatControl volume = (FloatControl) AudioManager.activeInputLine.line.getControl(FloatControl.Type.MASTER_GAIN);
            float gain = (opt_ivol.getValue() * 0.01f - 1.f) * volume.getMaximum() - Float.MIN_NORMAL;
            volume.setValue(gain);
            ConfigManager.setDoubleProperty("input." + AudioManager.activeInput.getMixerInfo().getName() + ".gain", gain);
        });
        add(opt_ivol);
        add(fieldLabel("Output volume"));
        opt_ovol = new JSlider(0, 200, 100);
        opt_ovol.setMajorTickSpacing(100);
        opt_ovol.setMinorTickSpacing(20);
        opt_ovol.setPaintTicks(true);
        opt_ovol.addChangeListener(e -> {
            FloatControl volume = (FloatControl) AudioManager.activeOutputLine.line.getControl(FloatControl.Type.MASTER_GAIN);
            float gain = (opt_ovol.getValue() * 0.01f - 1.f) * volume.getMaximum() - Float.MIN_NORMAL;
            volume.setValue(gain);
            ConfigManager.setDoubleProperty("output." + AudioManager.activeOutput.getMixerInfo().getName() + ".gain", gain);
        });
        add(opt_ovol);
    }
    private JLabel fieldLabel(String name) {
        JLabel label = new JLabel(name);
        label.setHorizontalAlignment(JLabel.RIGHT);
        return label;
    }
    
    static void updateAudio() {
        if (dialog == null) return;
        dialog.selectionLock = true;
        dialog.opt_idev.removeAllItems();
        Mixer activeInput = AudioManager.activeInput;
        for (Mixer mixer : AudioManager.inputDevices)
            dialog.opt_idev.addItem(mixer.getMixerInfo().getName());
        if (AudioManager.inputDevices.isEmpty()) {
            dialog.opt_idev.addItem("No input devices found");
            dialog.opt_idev.setSelectedIndex(0);
            dialog.opt_idev.setEnabled(false);
        } else {
            dialog.opt_idev.setSelectedItem(activeInput.getMixerInfo().getName());
            dialog.opt_idev.setEnabled(true);
        }
        dialog.opt_odev.removeAllItems();
        for (Mixer mixer : AudioManager.outputDevices)
            dialog.opt_odev.addItem(mixer.getMixerInfo().getName());
        if (AudioManager.outputDevices.isEmpty()) {
            dialog.opt_odev.addItem("No output devices found");
            dialog.opt_odev.setSelectedIndex(0);
            dialog.opt_odev.setEnabled(false);
        } else {
            dialog.opt_odev.setSelectedItem(AudioManager.activeOutput.getMixerInfo().getName());
            dialog.opt_odev.setEnabled(true);
        }
        dialog.selectionLock = false;
        if (AudioManager.activeInputLine.line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl volume = (FloatControl) AudioManager.activeInputLine.line.getControl(FloatControl.Type.MASTER_GAIN);
            dialog.opt_ivol.setValue((int) (volume.getValue() * 100.f / volume.getMaximum() + 100.f));
        } else {
            dialog.opt_ivol.setValue(100);
            dialog.opt_ivol.setEnabled(false);
        }
        if (AudioManager.activeOutputLine.line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl volume = (FloatControl) AudioManager.activeOutputLine.line.getControl(FloatControl.Type.MASTER_GAIN);
            dialog.opt_ovol.setValue((int) (volume.getValue() * 100.f / volume.getMaximum() + 100.f));
        } else {
            dialog.opt_ovol.setValue(100);
            dialog.opt_ovol.setEnabled(false);
        }
        
    }
    static synchronized void showDialog() {
        if (dialog == null) {
            dialog = new AudioSettingsDialog();
            updateAudio();
        }
        dialog.setVisible(true);
    }
    
    @Override
    public Insets getInsets() {
        final Insets sup = super.getInsets();
        return new Insets(sup.top + 0, sup.left - 100, sup.right + 10, sup.bottom + 10);
    }
}
