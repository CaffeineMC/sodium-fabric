package me.jellysquid.mods.sodium.client.render.chunk.compile.tasks;

import org.joml.Vector3f;
import org.joml.Vector3fc;

import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.compile.BuilderTaskOutput;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildContext;
import me.jellysquid.mods.sodium.client.util.task.CancellationToken;

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
public abstract class ChunkBuilderTask<OUTPUT extends BuilderTaskOutput> {
    protected final RenderSection render;
    protected final int submitTime;
    protected final Vector3fc cameraPos;

    public ChunkBuilderTask(RenderSection render, int time, Vector3fc absoluteCameraPos) {
        this.render = render;
        this.submitTime = time;
        this.cameraPos = new Vector3f(
                absoluteCameraPos.x() - (float) render.getOriginX(),
                absoluteCameraPos.y() - (float) render.getOriginY(),
                absoluteCameraPos.z() - (float) render.getOriginZ());
    }

    /**
     * Executes the given build task asynchronously from the calling thread. The implementation should be careful not
     * to access or modify global mutable state.
     *
     * @param context            The context to use for building this chunk
     * @param cancellationToken The cancellation source which can be used to query if the task is cancelled
     * @return The build result of this task, containing any data which needs to be uploaded on the main-thread, or null
     *         if the task was cancelled.
     */
    public abstract OUTPUT execute(ChunkBuildContext context, CancellationToken cancellationToken);
}
