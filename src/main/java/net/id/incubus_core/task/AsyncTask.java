package net.id.incubus_core.task;

import net.minecraft.util.Identifier;

import javax.annotation.Nullable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class AsyncTask extends AbstractTask {

    @Nullable
    protected Future<?> future;

    public AsyncTask(Identifier taskId, Runnable taskRunnable, Runnable cancelRunnable) {
        super(taskId, taskRunnable, cancelRunnable);
    }

    public void submit(ExecutorService scheduler) {
        if(scheduler != null)
            future = scheduler.submit(taskRunnable);
    }

    @Override
    public boolean cancel(boolean failHard) {
        if(!failHard)
            cancelRunnable.run();
        if(future != null)
            return future.cancel(true);
        return false;
    }

    @Override
    public boolean isActive() {
        if(future != null)
            return !future.isDone();
        return false;
    }

    @Override
    public boolean isAsync() {
        return true;
    }
}
