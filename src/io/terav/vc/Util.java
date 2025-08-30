package io.terav.vc;

public class Util {
    private Util() {}
    
    /**
     * Suspends thread execution for the specified delay
     * @param delay The delay to suspend execution for, in milliseconds
     * @return {@code false} if the thread was interrupted whilst waiting
     */
    public static boolean sleep(int delay) {
        if (delay < 0) throw new IllegalArgumentException("Delay cannot be negative");
        return sleepUntil(System.currentTimeMillis() + delay);
    }
    /**
     * Suspends thread execution for the specified delay, ignoring interrupts
     * @param delay The delay to suspend execution for, in milliseconds
     * @return {@code true} if an interrupt was issued during this sleep
     */
    public static boolean forceSleep(int delay) {
        if (delay < 0) throw new IllegalArgumentException("Delay cannot be negative");
        final long end = System.currentTimeMillis() + delay;
        boolean interrupted = false;
        while (System.currentTimeMillis() < end)
            interrupted = interrupted || !sleepUntil(end);
        return !interrupted;
    }
    /**
     * Suspends thread execution until the specified time
     * @param time The time to resume execution, according to {@link java.lang.System#currentTimeMillis()}
     * @return {@code false} if the thread was suspended for the requested duration, or {@code true} if it was interrupted
     */
    public static boolean sleepUntil(long time) {
        final long now = System.currentTimeMillis();
        if (now >= time) return false;
        try {
            Thread.sleep(time - now);
            return false;
        } catch (InterruptedException e) {
            return true;
        }
    }
}
