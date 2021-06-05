package me.jellysquid.mods.sodium.client.render.chunk.tasks;

import me.jellysquid.mods.sodium.client.render.chunk.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.pipeline.context.ChunkRenderCacheLocal;
import me.jellysquid.mods.sodium.client.util.task.CancellationSource;

/**
 * Build tasks are immutable jobs (with optional prioritization) which contain all the necessary state to perform
 * chunk mesh updates or quad sorting off the main thread.
 *
 * When a task is constructed on the main thread, it should copy all the state it requires in order to complete the task
 * without further synchronization. The task will then be scheduled for async execution on a thread pool.
 *
 * After the task completes, it returns a "build result" which contains any computed data that needs to be handled
 * on the main thread.
 *
 * @param <T> The graphics state of the chunk render
 */
public abstract class ChunkRenderBuildTask<T extends ChunkGraphicsState> {
    /**
     * Executes the given build task asynchronously from the calling thread. The implementation should be careful not
     * to access or modify global mutable state.
     *
     * @param cache The render cache to use for building this chunk
     * @param buffers The temporary scratch buffers for rendering block data
     * @param cancellationSource The cancellation source which can be used to query if the task is cancelled
     * @return The build result of this task, containing any data which needs to be uploaded on the main-thread, or null
     *         if the task was cancelled.
     */
    public abstract ChunkBuildResult<T> performBuild(ChunkRenderCacheLocal cache, ChunkBuildBuffers buffers,
                                                     CancellationSource cancellationSource);

    /**
     * Called on the main render thread when the task's execution returns. The implementation should release any
     * resources it's still holding onto at this point.
     */
    public abstract void releaseResources();
}
