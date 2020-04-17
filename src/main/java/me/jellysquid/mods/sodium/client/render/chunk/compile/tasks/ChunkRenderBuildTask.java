package me.jellysquid.mods.sodium.client.render.chunk.compile.tasks;

import me.jellysquid.mods.sodium.client.render.backends.ChunkRenderState;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.pipeline.ChunkRenderPipeline;

public abstract class ChunkRenderBuildTask<T extends ChunkRenderState> {
    public abstract ChunkBuildResult<T> performBuild(ChunkRenderPipeline pipeline, ChunkBuildBuffers buffers);

    public abstract void releaseResources();
}
