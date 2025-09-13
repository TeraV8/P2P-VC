package io.terav.vc;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.swing.JOptionPane;

public class AudioManager {
    static final List<Mixer> inputDevices = new ArrayList<>();
    static final List<Mixer> outputDevices = new ArrayList<>();
    static Mixer activeInput = null;
    static Mixer activeOutput = null;
    static StreamInputDriver activeInputLine = null;
    static StreamOutputDriver activeOutputLine = null;
    static Consumer<byte[]> activeInputConsumer = null;
    static final AudioFormat audioFormat = new AudioFormat(24000.0f, 8, 1, true, false);
    static int bufferSize = (int) (audioFormat.getSampleRate() * 0.040); // 40 millis
    
    static void start() {
        String lastInputDevice = ConfigManager.getStringProperty("input.device", "nonexistent device");
        String lastOutputDevice = ConfigManager.getStringProperty("output.device", "nonexistent device");
        
        scanForDevices();
        
        if (!inputDevices.isEmpty()) {
            for (Mixer input : inputDevices) {
                if (input.getMixerInfo().getName().equals(lastInputDevice))
                    setActiveInput(input);
            }
            if (activeInput == null)
                setActiveInput(inputDevices.get(0));
        }
        if (!outputDevices.isEmpty()) {
            for (Mixer output : outputDevices) {
                if (output.getMixerInfo().getName().equals(lastOutputDevice)) {
                    setActiveOutput(output);
                }
            }
            if (activeOutput == null)
                setActiveOutput(outputDevices.get(0));
        }
    }
    static void scanForDevices() {
        Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
        Mixer[] mixers = new Mixer[mixerInfo.length];
        inputDevices.clear();
        outputDevices.clear();
        for (int i = 0; i < mixerInfo.length; i++) {
            mixers[i] = AudioSystem.getMixer(mixerInfo[i]);
            if (mixers[i].isLineSupported(new Line.Info(SourceDataLine.class)))
                outputDevices.add(mixers[i]);
            if (mixers[i].isLineSupported(new Line.Info(TargetDataLine.class)))
                inputDevices.add(mixers[i]);
        }
        if (outputDevices.isEmpty())
            JOptionPane.showMessageDialog(null, "No audio output devices found!", "No audio output", JOptionPane.ERROR_MESSAGE);
        if (inputDevices.isEmpty())
            JOptionPane.showMessageDialog(null, "No audio input devices found!", "No audio input", JOptionPane.WARNING_MESSAGE);
        if (!inputDevices.contains(activeInput))
            if (!inputDevices.isEmpty())
                setActiveInput(inputDevices.get(0));
            else
                setActiveInput(null);
        if (!outputDevices.contains(activeOutput))
            if (!outputDevices.isEmpty())
                setActiveOutput(outputDevices.get(0));
            else
                setActiveOutput(null);
    }
    static synchronized void setActiveInput(Mixer m) {
        if (m == null)
            throw new NullPointerException();
        if (!inputDevices.contains(m))
            throw new IllegalArgumentException("Device not registered");
        if (activeInput == m) return;
        if (activeInput != null)
            activeInputLine.stop();
        try {
            TargetDataLine sdl = (TargetDataLine) m.getLine(new Line.Info(TargetDataLine.class));
            sdl.open(audioFormat, bufferSize);
            activeInputLine = new StreamInputDriver(sdl, b -> {
                if (activeInputConsumer != null) activeInputConsumer.accept(ConfigManager.getBooleanProperty("input.mute", false) ? new byte[0] : b);
            });
            sdl.start();
            activeInputLine.thread.start();
            if (activeInputLine.line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl volume = (FloatControl) AudioManager.activeInputLine.line.getControl(FloatControl.Type.MASTER_GAIN);
                volume.setValue(Math.min((float) ConfigManager.getDoubleProperty("input." + m.getMixerInfo().getName() + ".gain", 0.d), volume.getMaximum()));
            }
            ConfigManager.setStringProperty("input.device", m.getMixerInfo().getName());
        } catch (LineUnavailableException e) {
            activeInput = null;
            return;
        }
        activeInput = m;
        AudioSettingsDialog.updateAudio();
    }
    static synchronized void setActiveOutput(Mixer m) {
        if (m == null)
            throw new NullPointerException();
        if (!outputDevices.contains(m))
            throw new IllegalArgumentException("Device not registered");
        if (activeOutput == m) return;
        if (activeOutput != null)
            activeOutputLine.stop();
        try {
            SourceDataLine sdl = (SourceDataLine) m.getLine(new Line.Info(SourceDataLine.class));
            sdl.open(audioFormat);
            activeOutputLine = new StreamOutputDriver(sdl);
            sdl.start();
            activeOutputLine.thread.start();
            if (activeOutputLine.line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl volume = (FloatControl) AudioManager.activeOutputLine.line.getControl(FloatControl.Type.MASTER_GAIN);
                volume.setValue(Math.min((float) ConfigManager.getDoubleProperty("output." + m.getMixerInfo().getName() + ".gain", 0.d), volume.getMaximum()));
            }
            ConfigManager.setStringProperty("output.device", m.getMixerInfo().getName());
        } catch (LineUnavailableException e) {
            activeOutput = null;
            return;
        }
        activeOutput = m;
        AudioSettingsDialog.updateAudio();
    }
    public static void setActiveInputConsumer(Consumer<byte[]> consumer) {
        activeInputConsumer = consumer;
    }
    public static StreamInputDriver getInputDriver() {
        return activeInputLine;
    }
    public static StreamOutputDriver getOutputDriver() {
        return activeOutputLine;
    }
}
