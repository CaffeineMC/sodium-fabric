package me.jellysquid.mods.sodium.client.render.chunk.compile;

import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionMeshParts;
import me.jellysquid.mods.sodium.client.render.chunk.gfni.TranslucentData;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionInfo;

import java.util.Map;

/**
 * The result of a chunk rebuild task which contains any and all data that needs to be processed or uploaded on
 * the main thread. If a task is cancelled after finishing its work and not before the result is processed, the result
 * will instead be discarded.
 */
public class ChunkBuildOutput extends ChunkSortOutput {
    public final BuiltSectionInfo info;

    public final Map<TerrainRenderPass, BuiltSectionMeshParts> meshes;

    public ChunkBuildOutput(RenderSection render, int buildTime, TranslucentData translucentData, BuiltSectionInfo info, Map<TerrainRenderPass, BuiltSectionMeshParts> meshes) {
        super(render, buildTime, translucentData);

        this.info = info;
        this.meshes = meshes;
    }

    public BuiltSectionMeshParts getMesh(TerrainRenderPass pass) {
        return this.meshes.get(pass);
    }

    @Override
    public void deleteAfterUpload() {
        super.deleteAfterUpload();
        for (BuiltSectionMeshParts data : this.meshes.values()) {
            data.getVertexData().free();
        }
    }
}
