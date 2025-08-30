package io.terav.vc;

import java.util.ArrayList;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.swing.JOptionPane;

public class TestMain2 {
    public static final AudioFormat audioFormat = new AudioFormat(24000.0f, 8, 1, true, false);
    static StreamInputDriver idrv;
    static StreamOutputDriver odrv;
    static TargetDataLine iline;
    static SourceDataLine oline;
    
    public static void main(String[] args) throws LineUnavailableException {
        Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
        Mixer[] mixer = new Mixer[mixerInfo.length];
        ArrayList<Mixer> sourceMixers = new ArrayList<>();
        ArrayList<Mixer> targetMixers = new ArrayList<>();
        for (int i = 0; i < mixerInfo.length; i++) {
            mixer[i] = AudioSystem.getMixer(mixerInfo[i]);
            if (mixer[i].isLineSupported(new Line.Info(SourceDataLine.class)))
                sourceMixers.add(mixer[i]);
            if (mixer[i].isLineSupported(new Line.Info(TargetDataLine.class)))
                targetMixers.add(mixer[i]);
        }
        System.out.println("Source mixers:");
        for (Mixer m : sourceMixers)
            System.out.println("\t" + m.getMixerInfo());
        System.out.println("Target mixers:");
        for (Mixer m : targetMixers)
            System.out.println("\t" + m.getMixerInfo());
        if (sourceMixers.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Cannot find an audio output device!", "No Audio Output", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (targetMixers.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Cannot find an audio input device!", "No Audio Input", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Mixer outputDevice = null;
        String[] opts = new String[sourceMixers.size()];
        for (int i = 0; i < opts.length; i++) {
            opts[i] = sourceMixers.get(i).getMixerInfo().getName();
        }
        Object selection = JOptionPane.showInputDialog(null, "Select an audio output device:", "Select Audio Output", JOptionPane.QUESTION_MESSAGE, null, opts, null);
        if (selection == null) return;
        for (int i = 0; i < opts.length; i++) {
            if (opts[i] == selection)
                outputDevice = sourceMixers.get(i);
        }
        System.out.println("Using output device: " + outputDevice.getMixerInfo());
        
        Mixer inputDevice = null;
        opts = new String[targetMixers.size()];
        for (int i = 0; i < opts.length; i++) {
            opts[i] = targetMixers.get(i).getMixerInfo().getName();
        }
        selection = JOptionPane.showInputDialog(null, "Select an audio input device:", "Select Audio Input", JOptionPane.QUESTION_MESSAGE, null, opts, null);
        if (selection == null) return;
        for (int i = 0; i < opts.length; i++) {
            if (opts[i] == selection)
                inputDevice = targetMixers.get(i);
        }
        System.out.println("Using input device: " + inputDevice.getMixerInfo());
        
        iline = (TargetDataLine) inputDevice.getLine(new Line.Info(TargetDataLine.class));
        oline = (SourceDataLine) outputDevice.getLine(new Line.Info(SourceDataLine.class));
        int bufferSize = (int) (audioFormat.getSampleRate() * 0.04f); // 50 millisecond buffer
        iline.open(audioFormat, bufferSize);
        oline.open(audioFormat);
        idrv = new StreamInputDriver(iline, d -> {
            odrv.dispatch(d);
        });
        odrv = new StreamOutputDriver(oline);
        
        // let's test this
        oline.start();
        odrv.thread.start();
        iline.start();
        idrv.thread.start();
        try {
            Thread.sleep(15000);
        } catch (InterruptedException e) {}
        
        iline.stop();
        oline.stop();
        oline.flush();
        iline.close();
        oline.close();
    }
}
