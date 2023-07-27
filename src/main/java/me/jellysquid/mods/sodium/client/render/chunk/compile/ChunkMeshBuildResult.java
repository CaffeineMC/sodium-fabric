package me.jellysquid.mods.sodium.client.render.chunk.compile;

import java.util.Map;

import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;

public class ChunkMeshBuildResult extends ChunkBuildResult {
    public final ChunkRenderData data;
    public final Map<TerrainRenderPass, ChunkMeshData> meshes;

    public ChunkMeshBuildResult(RenderSection render, int buildTime, ChunkRenderData data,
            Map<TerrainRenderPass, ChunkMeshData> meshes) {
        super(render, buildTime);
        this.data = data;
        this.meshes = meshes;
    }

    public ChunkMeshData getMesh(TerrainRenderPass pass) {
        return this.meshes.get(pass);
    }

    @Override
    public void setDataOn(RenderSection section) {
        section.setData(this.data);
    }

    @Override
    public void delete() {
        for (ChunkMeshData data : this.meshes.values()) {
            data.getVertexData().free();
        }
    }
}
