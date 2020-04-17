package me.jellysquid.mods.sodium.client.render.chunk.compile.tasks;

import me.jellysquid.mods.sodium.client.render.backends.ChunkRenderState;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRender;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.pipeline.ChunkRenderPipeline;

public class ChunkRenderEmptyBuildTask<T extends ChunkRenderState> extends ChunkRenderBuildTask<T> {
    private final ChunkRender<T> render;

    public ChunkRenderEmptyBuildTask(ChunkRender<T> render) {
        this.render = render;
    }

    @Override
    public ChunkBuildResult<T> performBuild(ChunkRenderPipeline pipeline, ChunkBuildBuffers buffers) {
        return new ChunkBuildResult<>(this.render, ChunkRenderData.EMPTY);
    }

    @Override
    public void releaseResources() {

    }
}
