package me.jellysquid.mods.sodium.client.render.chunk.compile;

import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderCache;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;

public class ChunkBuildContext {
    public final ChunkBuildBuffers buffers;
    public final BlockRenderCache cache;
    public final ChunkVertexType vertexType;

    public ChunkBuildContext(ClientWorld world, ChunkVertexType vertexType) {
        this.buffers = new ChunkBuildBuffers(vertexType);
        this.cache = new BlockRenderCache(MinecraftClient.getInstance(), world);
        this.vertexType = vertexType;
    }

    public void cleanup() {
        this.buffers.destroy();
        this.cache.cleanup();
    }
}
