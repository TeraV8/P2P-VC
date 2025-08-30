package io.terav.vc;

import javax.sound.sampled.SourceDataLine;

public class StreamOutputDriver implements Runnable {
    public final SourceDataLine line;
    public final Thread thread;
    private byte[] nextBlock;
//    private Thread pendingDispatch;
    private boolean stopped = false;
    public boolean silenced = false;
    
    public StreamOutputDriver(SourceDataLine lineOut) {
        if (lineOut == null) throw new NullPointerException();
        this.line = lineOut;
        this.thread = new Thread(this);
        this.thread.setName("AudioOutputDriver");
        this.thread.setDaemon(true);
    }
    
    public synchronized void dispatch(byte[] data) {
//        if (nextBlock == null) {
//            nextBlock = data;
//            thread.interrupt();
//            return;
//        }
//        pendingDispatch = Thread.currentThread();
//        while (nextBlock != null) {
//            if (stopped) return;
//            try {
//                Thread.sleep(1);
//            } catch (InterruptedException e) {}
//        }
//        pendingDispatch = null;
        nextBlock = data;
        thread.interrupt();
    }
    
    @Override
    public void run() {
        while (line.isOpen()) {
            // write audio data
            if (nextBlock != null) {
                line.write(nextBlock, 0, nextBlock.length);
                nextBlock = null;
//                if (pendingDispatch != null) pendingDispatch.interrupt();
            } else {
                if (silenced && line.available() < 24)
                    line.write(new byte[25], 0, 25);
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {}
            }
        }
    }
    
    public void stop() {
        stopped = true;
        line.stop();
        line.close();
    }
}
