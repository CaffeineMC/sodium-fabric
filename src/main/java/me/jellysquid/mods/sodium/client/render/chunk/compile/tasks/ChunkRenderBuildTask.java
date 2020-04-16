package me.jellysquid.mods.sodium.client.render.chunk.compile.tasks;

import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.pipeline.ChunkRenderPipeline;

public abstract class ChunkRenderBuildTask {
    public abstract ChunkRenderUploadTask performBuild(ChunkRenderPipeline pipeline, ChunkBuildBuffers buffers);
}
