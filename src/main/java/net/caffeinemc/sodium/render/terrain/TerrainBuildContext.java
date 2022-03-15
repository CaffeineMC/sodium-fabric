package net.caffeinemc.sodium.render.terrain;

import net.caffeinemc.sodium.render.chunk.compile.tasks.TerrainBuildBuffers;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPassManager;
import net.caffeinemc.sodium.render.terrain.context.PreparedTerrainRenderCache;
import net.caffeinemc.sodium.render.terrain.format.TerrainVertexType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.world.World;

public class TerrainBuildContext {
    public final TerrainBuildBuffers buffers;
    public final PreparedTerrainRenderCache cache;

    public TerrainBuildContext(World world, TerrainVertexType vertexType, ChunkRenderPassManager renderPassManager) {
        this.buffers = new TerrainBuildBuffers(vertexType, renderPassManager);
        this.cache = new PreparedTerrainRenderCache(MinecraftClient.getInstance(), world);
    }

    public void release() {
        this.buffers.destroy();
    }
}
