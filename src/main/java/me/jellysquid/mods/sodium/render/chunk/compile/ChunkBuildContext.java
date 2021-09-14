package me.jellysquid.mods.sodium.render.chunk.compile;

import me.jellysquid.mods.sodium.model.vertex.type.ChunkVertexType;
import me.jellysquid.mods.sodium.render.renderer.TerrainRenderContext;
import net.minecraft.world.World;

public class ChunkBuildContext {
    public final TerrainRenderContext terrainRenderer;

    public ChunkBuildContext(World world, ChunkVertexType vertexType) {
        this.terrainRenderer = new TerrainRenderContext(world, vertexType);
    }

    public void release() {
        this.terrainRenderer.release();
    }
}
