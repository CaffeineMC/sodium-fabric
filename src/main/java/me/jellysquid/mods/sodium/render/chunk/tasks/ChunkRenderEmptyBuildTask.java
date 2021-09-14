package me.jellysquid.mods.sodium.render.chunk.tasks;

import me.jellysquid.mods.sodium.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.util.task.CancellationSource;
import me.jellysquid.mods.sodium.render.renderer.TerrainRenderContext;

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
    private final int detailLevel;

    public ChunkRenderEmptyBuildTask(RenderSection render, int frame, int detailLevel) {
        this.render = render;
        this.frame = frame;
        this.detailLevel = detailLevel;
    }

    @Override
    public ChunkBuildResult performBuild(TerrainRenderContext context, CancellationSource cancellationSource) {
        return new ChunkBuildResult(this.render, ChunkRenderData.EMPTY, Collections.emptyMap(), this.frame, this.detailLevel);
    }
}
