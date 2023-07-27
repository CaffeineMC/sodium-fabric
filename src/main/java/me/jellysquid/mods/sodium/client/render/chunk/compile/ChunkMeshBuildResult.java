package me.jellysquid.mods.sodium.client.render.chunk.compile;

import java.util.Map;

import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionInfo;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionMeshParts;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;

public class ChunkMeshBuildResult extends ChunkBuildResult {
    public final BuiltSectionInfo info;
    public final Map<TerrainRenderPass, BuiltSectionMeshParts> meshes;

    public ChunkMeshBuildResult(RenderSection render, int buildTime, BuiltSectionInfo info,
            Map<TerrainRenderPass, BuiltSectionMeshParts> meshes) {
        super(render, buildTime);
        this.info = info;
        this.meshes = meshes;
    }

    public BuiltSectionMeshParts getMesh(TerrainRenderPass pass) {
        return this.meshes.get(pass);
    }

    @Override
    public void setDataOn(RenderSection section) {
        section.setData(this.info);
    }

    @Override
    public void delete() {
        for (BuiltSectionMeshParts info : this.meshes.values()) {
            info.getVertexData().free();
        }
    }
}
