package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkMeshBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;

public class ChunkModelSlice {
    public final int start, len;
    public final BlockRenderPass pass;
    public final ChunkMeshBuilder builder;

    public ChunkModelSlice(BlockRenderPass pass, ChunkMeshBuilder builder, int start, int len) {
        this.builder = builder;
        this.pass = pass;
        this.start = start;
        this.len = len;
    }
}
