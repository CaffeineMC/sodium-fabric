package me.jellysquid.mods.sodium.client.render.chunk;

import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.util.math.BlockPos;

public interface ExtendedBuiltChunkStorage {
    ChunkBuilder.BuiltChunk bridge$getRenderedChunk(BlockPos pos);

    ChunkBuilder.BuiltChunk[] getData();

    void bridge$updateCameraPosition(double x, double z);
}
