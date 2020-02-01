package me.jellysquid.mods.sodium.client.render.chunk;

import net.minecraft.client.render.chunk.ChunkBuilder;

public interface ExtendedWorldRenderer {
    ChunkBuilder getChunkBuilder();

    ExtendedBuiltChunkStorage getBuiltChunkStorage();
}
