package net.id.incubus_core.task;

import net.minecraft.util.Identifier;

import java.util.concurrent.*;

public class ScheduledTask extends AsyncTask {

    private final TimeUnit timeUnit;
    private final long duration;
    private final boolean repeating;

    public ScheduledTask(Identifier id, long duration, TimeUnit timeUnit, Runnable taskRunnable, Runnable cancelRunnable, boolean repeating) {
        super(id, taskRunnable, cancelRunnable);
        this.duration = duration;
        this.timeUnit = timeUnit;
        this.repeating = repeating;
    }

    public void schedule(ScheduledExecutorService scheduler) {
        if(scheduler != null) {
            if (repeating)
                future = scheduler.scheduleAtFixedRate(taskRunnable, duration, duration, timeUnit);
            else
                future = scheduler.schedule(taskRunnable, duration, timeUnit);
        }
    }
}
