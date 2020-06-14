package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkMeshBuilder;

public class ChunkModelSlice {
    public final int start, len;
    public final ChunkMeshBuilder builder;

    public ChunkModelSlice(int start, int len, ChunkMeshBuilder builder) {
        this.start = start;
        this.len = len;
        this.builder = builder;
    }
}
