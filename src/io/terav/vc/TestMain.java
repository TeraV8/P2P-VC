package io.terav.vc;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Control;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class TestMain {
    static final float SAMPLE_RATE = 48000.0f;
    public static void main(String[] args) throws LineUnavailableException {
        SourceDataLine outputLine = (SourceDataLine) AudioSystem.getLine(new Line.Info(SourceDataLine.class));
        outputLine.open(new AudioFormat(SAMPLE_RATE, 8, 1, true, false));
        System.out.println(outputLine.getLineInfo());
        System.out.println(outputLine.getFormat());
        FloatControl gain = (FloatControl) outputLine.getControl(FloatControl.Type.MASTER_GAIN);
        if (gain == null)
            System.err.println("Can't control gain!");
        else
            gain.setValue(-22.0f);
        LStreamOutputDriver driver = new LStreamOutputDriver(outputLine);
        Thread t = new Thread(driver);
        t.setName("AudioOutputDriver");
        t.setDaemon(true);
        t.start();
        outputLine.start();
        while (true) {
            for (int i = 0; i < 40; i++) {
                driver.setRampFunction(200.0 + i * i, 0.1, 1.0);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {}
            }
            //for (int i = 40; i >= 0; i--) {
            //    driver.setRampFunction(200.0 + i * i, 0.5, 0.8);
            //    try {
            //        Thread.sleep(100);
            //    } catch (InterruptedException e) {}
            //}
        }
    }
}
class LStreamOutputDriver implements Runnable {
    final SourceDataLine outputLine;
    private byte[] cycle;
    
    LStreamOutputDriver(SourceDataLine sdl) {
        this.outputLine = sdl;
    }
    void setSquareFunction(double frequency, double dutyCycle, double amplitude) {
        byte[] data = new byte[(int) (TestMain.SAMPLE_RATE / frequency)];
        for (int i = 0; i < data.length; i++) {
            double phase = (double) i / data.length;
            if (phase < dutyCycle)
                data[i] = (byte) (amplitude * 127);
            else
                data[i] = (byte) (amplitude * -127);
        }
        this.cycle = data;
    }
    void setSineFunction(double frequency, double amplitude) {
        byte[] data = new byte[(int) (TestMain.SAMPLE_RATE / frequency)];
        for (int i = 0; i < data.length; i++) {
            double phase = (double) i / data.length;
            data[i] = (byte) (Math.sin(2.0 * Math.PI * phase) * amplitude * 127);
        }
        this.cycle = data;
    }
    void setRampFunction(double frequency, double symmetry, double amplitude) {
        byte[] data = new byte[(int) (TestMain.SAMPLE_RATE / frequency)];
        for (int i = 0; i < data.length; i++) {
            double phase = (double) i / data.length;
            if (phase < symmetry) {
                data[i] = (byte) (phase * 254.0 / symmetry - 127.0);
            } else if (phase > symmetry) {
                data[i] = (byte) ((symmetry - phase) * 254.0 / (1.0 - symmetry) + 127.0);
            } else data[i] = (byte) (amplitude * 127);
        }
        this.cycle = data;
    }
    @Override
    public void run() {
        while (outputLine.isOpen()) {
            if (this.cycle == null) continue;
            this.outputLine.write(cycle, 0, cycle.length);
        }
    }
}
