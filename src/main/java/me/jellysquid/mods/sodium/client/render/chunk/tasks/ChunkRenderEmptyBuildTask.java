package me.jellysquid.mods.sodium.client.render.chunk.tasks;

import me.jellysquid.mods.sodium.client.gl.compile.ChunkBuildContext;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.util.task.CancellationSource;

import java.util.Collections;

/**
 * A build task which does no computation and always return an empty build result. These tasks are created whenever
 * chunk meshes need to be deleted as the only way to change graphics state is to send a message to the main
 * actor thread. In cases where new chunk renders are being created and scheduled, the scheduler will prefer to just
 * synchronously update the render's data to an empty state to speed things along.
 */
public class ChunkRenderEmptyBuildTask extends ChunkRenderBuildTask {
    private final RenderSection render;
    private final int frame;

    public ChunkRenderEmptyBuildTask(RenderSection render, int frame) {
        this.render = render;
        this.frame = frame;
    }

    @Override
    public ChunkBuildResult performBuild(ChunkBuildContext context, CancellationSource cancellationSource) {
        return new ChunkBuildResult(this.render, ChunkRenderData.EMPTY, Collections.emptyMap(), this.frame);
    }

    @Override
    public void releaseResources() {

    }
}
