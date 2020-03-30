package me.jellysquid.mods.sodium.client.render.chunk.compile;

public abstract class ChunkRenderBuildTask {
    public abstract ChunkRenderUploadTask performBuild(VertexBufferCache buffers);
}
