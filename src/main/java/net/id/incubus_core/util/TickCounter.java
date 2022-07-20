package net.id.incubus_core.util;

public class TickCounter {

    private final int TICK_THRESHOLD;
    private final int MAX_VALUE;

    private int counter = 0;

    /**
     * Placed in tickable methods acts as a basic counter
     *
     * @param tickThreshold Number of ticks before test() returns true and the counter is reset
     * @param maxValue Maximum number of ticks before the counter is reset. Only called if test() isn't first
     */
    public TickCounter(int tickThreshold, int maxValue) {
        TICK_THRESHOLD = tickThreshold;
        MAX_VALUE = maxValue;
    }

    public TickCounter(int tickThreshold) {
        this(tickThreshold, Integer.MAX_VALUE);
    }

    /**
     * @return Whether the counter has passed the tick threshold
     */
    public boolean test() {
        if(counter >= TICK_THRESHOLD) {
            reset();
            return true;
        }
        return false;
    }

    /**
     * Increment the tick counter. Required for each iteration of a tickable loop
     */
    public void increment() {
        if(counter >= MAX_VALUE || counter < 0) reset();
        else counter++;
    }

    public void reset() {
        counter = 0;
    }

    public int value() { return this.counter; }
}
