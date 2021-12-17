package me.jellysquid.mods.sodium.client.gl.compile;

import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderLayerManager;
import me.jellysquid.mods.sodium.client.render.pipeline.context.ChunkRenderCacheLocal;
import net.minecraft.client.MinecraftClient;
import net.minecraft.world.World;

public class ChunkBuildContext {
    public final ChunkBuildBuffers buffers;
    public final ChunkRenderCacheLocal cache;

    public ChunkBuildContext(World world, BlockRenderLayerManager renderPassManager) {
        this.buffers = new ChunkBuildBuffers(renderPassManager);
        this.cache = new ChunkRenderCacheLocal(MinecraftClient.getInstance(), world);
    }

    public void destroy() {
        this.buffers.destroy();
    }
}
