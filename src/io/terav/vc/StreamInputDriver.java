package io.terav.vc;

import java.util.function.Consumer;
import javax.sound.sampled.TargetDataLine;

public class StreamInputDriver implements Runnable {
    public final TargetDataLine line;
    private final Consumer<byte[]> output;
    public final int bufferSize;
    public final Thread thread;
    private boolean stopped = false;
    
    public StreamInputDriver(TargetDataLine lineIn, Consumer<byte[]> consumer) {
        if (lineIn == null) throw new NullPointerException();
        this.line = lineIn;
        this.bufferSize = this.line.getBufferSize();
        if (consumer == null) throw new NullPointerException();
        this.output = consumer;
        this.thread = new Thread(this);
        this.thread.setName("AudioInputDriver");
        this.thread.setDaemon(true);
    }
    
    @Override
    public void run() {
        while (line.isOpen() && !stopped) {
            byte[] data = new byte[this.bufferSize];
            int count = line.read(data, 0, bufferSize);
            if (count > 0) {
                if (count < data.length) {
                    byte[] t = new byte[count];
                    System.arraycopy(data, 0, t, 0, count);
                    data = t;
                }
                output.accept(data);
            }
            if (count < this.bufferSize) {
                // line was (probably) closed or stopped
            }
        }
    }
    
    public void stop() {
        stopped = true;
        line.stop();
        line.close();
    }
}
