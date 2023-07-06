package me.jellysquid.mods.sodium.client.render.chunk.tasks;

import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildContext;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.util.task.CancellationSource;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Build tasks are immutable jobs (with optional prioritization) which contain all the necessary state to perform
 * chunk mesh updates or quad sorting off the main thread.
 *
 * When a task is constructed on the main thread, it should copy all the state it requires in order to complete the task
 * without further synchronization. The task will then be scheduled for async execution on a thread pool.
 *
 * After the task completes, it returns a "build result" which contains any computed data that needs to be handled
 * on the main thread.
 */
public abstract class ChunkRenderBuildTask {
    /**
     * Executes the given build task asynchronously from the calling thread. The implementation should be careful not
     * to access or modify global mutable state.
     *
     * @param context The context to use for building this chunk
     * @param cancellationSource The cancellation source which can be used to query if the task is cancelled
     * @return The build result of this task, containing any data which needs to be uploaded on the main-thread, or null
     *         if the task was cancelled.
     */
    public abstract ChunkBuildResult performBuild(ChunkBuildContext context, CancellationSource cancellationSource);

    public abstract void releaseResources();

    private final AtomicBoolean canceled = new AtomicBoolean();
    private final AtomicBoolean executing = new AtomicBoolean();
    private volatile ChunkBuildResult result;

    /**
     * Atomicly gets if the task was canceled
     * @return if the task was canceled
     */
    public boolean isCancelled() {
        return canceled.get();
    }

    public boolean cancel() {
        return canceled.getAndSet(true);
    }

    /**
     * Atomically gets and sets if the task is executing or has been executed, if false the task wasnt being executed but now is
     * @return if the task is currently executing or has been executed, returns false if it wasnt previously executing
     */
    public boolean execute() {
        return executing.getAndSet(true);
    }

    public boolean isExecuting() {
        return executing.get();
    }

    public void complete(ChunkBuildResult result) {
        this.result = result;
    }

    public boolean isComplete() {
        return result != null;
    }

    public ChunkBuildResult getResult() {
        return result;
    }

    public boolean important;
}
