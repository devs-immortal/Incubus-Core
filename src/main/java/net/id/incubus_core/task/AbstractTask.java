package net.id.incubus_core.task;

import net.minecraft.util.Identifier;

public abstract class AbstractTask implements TaskInfo {

    protected final Identifier taskId;
    protected final Runnable taskRunnable, cancelRunnable;

    /**
     * Abstract task foundation class. Used primarily to allow storing and retrieving different
     * types of tasks from a single place
     *
     * @param taskId Unique task ID, used for storing and retrieval
     * @param taskRunnable Task to run on completion
     * @param cancelRunnable Task to run in the case of a soft fail
     */
    public AbstractTask(Identifier taskId, Runnable taskRunnable, Runnable cancelRunnable) {
        this.taskId = taskId;
        this.taskRunnable = taskRunnable;
        this.cancelRunnable = cancelRunnable;
    }

    /**
     * Cancel the existing running task if one exists
     *
     * @param failHard A hard fail interrupts the task and doesn't trigger the cancel runnable task
     * @return Whether the task was cancelled successfully
     */
    public abstract boolean cancel(boolean failHard);
}
