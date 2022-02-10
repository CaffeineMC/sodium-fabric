package me.jellysquid.mods.sodium.render.terrain;

import me.jellysquid.mods.sodium.render.chunk.compile.tasks.TerrainBuildBuffers;
import me.jellysquid.mods.sodium.render.chunk.passes.ChunkRenderPassManager;
import me.jellysquid.mods.sodium.render.terrain.context.PreparedTerrainRenderCache;
import me.jellysquid.mods.sodium.render.terrain.format.TerrainVertexType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;

public class TerrainBuildContext {
    public final TerrainBuildBuffers buffers;
    public final PreparedTerrainRenderCache cache;

    public TerrainBuildContext(Level world, TerrainVertexType vertexType, ChunkRenderPassManager renderPassManager) {
        this.buffers = new TerrainBuildBuffers(vertexType, renderPassManager);
        this.cache = new PreparedTerrainRenderCache(Minecraft.getInstance(), world);
    }

    public void release() {
        this.buffers.destroy();
    }
}
