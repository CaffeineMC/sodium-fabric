package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.render.backends.ChunkRenderState;

public class ChunkBuildResult<T extends ChunkRenderState> {
    public final ChunkRender<T> render;
    public final ChunkRenderData data;

    public ChunkBuildResult(ChunkRender<T> render, ChunkRenderData data) {
        this.render = render;
        this.data = data;
    }
}
