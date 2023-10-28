package me.jellysquid.mods.sodium.client.render.chunk.compile;

import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionMeshParts;
import me.jellysquid.mods.sodium.client.render.chunk.gfni.PresentTranslucentData;
import me.jellysquid.mods.sodium.client.render.chunk.gfni.TranslucentData;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionInfo;

import java.util.Map;

/**
 * The result of a chunk rebuild task which contains any and all data that needs
 * to be processed or uploaded on the main thread. If a task is cancelled after
 * finishing its work and not before the result is processed, the result will
 * instead be discarded.
 */
public class ChunkBuildOutput extends BuilderTaskOutput implements OutputWithIndexData {
    public final BuiltSectionInfo info;
    public final TranslucentData translucentData;
    public final Map<TerrainRenderPass, BuiltSectionMeshParts> meshes;

    public ChunkBuildOutput(RenderSection render, int buildTime, TranslucentData translucentData, BuiltSectionInfo info,
            Map<TerrainRenderPass, BuiltSectionMeshParts> meshes) {
        super(render, buildTime);

        this.info = info;
        this.translucentData = translucentData;
        this.meshes = meshes;
    }

    public BuiltSectionMeshParts getMesh(TerrainRenderPass pass) {
        return this.meshes.get(pass);
    }

    @Override
    public PresentTranslucentData getTranslucentData() {
        if (this.translucentData instanceof PresentTranslucentData present) {
            return present;
        }
        return null;
    }

    @Override
    public void deleteAfterUpload() {
        super.deleteAfterUpload();

        // delete translucent data if it's not persisted for dynamic sorting
        if (this.translucentData != null && !this.translucentData.getSortType().needsTrigger) {
            this.translucentData.delete();
        }

        for (BuiltSectionMeshParts data : this.meshes.values()) {
            data.getVertexData().free();
        }
    }

    @Override
    public void deleteFully() {
        super.deleteFully();
        if (this.translucentData != null) {
            this.translucentData.delete();
        }
    }
}
