package me.jellysquid.mods.sodium.render.chunk.compile.tasks;

import me.jellysquid.mods.sodium.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.render.chunk.passes.ChunkRenderPass;
import me.jellysquid.mods.sodium.render.chunk.state.ChunkMesh;
import me.jellysquid.mods.sodium.render.chunk.state.ChunkRenderData;

import java.util.Collections;
import java.util.Map;

/**
 * The result of a chunk rebuild task which contains any and all data that needs to be processed or uploaded on
 * the main thread. If a task is cancelled after finishing its work and not before the result is processed, the result
 * will instead be discarded.
 */
public class TerrainBuildResult {
    public final RenderSection render;
    public final ChunkRenderData data;
    public final Map<ChunkRenderPass, ChunkMesh> meshes;
    public final int buildTime;

    public TerrainBuildResult(RenderSection render, ChunkRenderData data, Map<ChunkRenderPass, ChunkMesh> meshes, int buildTime) {
        this.render = render;
        this.data = data;
        this.meshes = meshes;
        this.buildTime = buildTime;
    }

    public Iterable<Map.Entry<ChunkRenderPass, ChunkMesh>> getMeshes() {
        return Collections.unmodifiableSet(this.meshes.entrySet());
    }

    public void delete() {
        for (ChunkMesh data : this.meshes.values()) {
            data.getVertexData()
                    .delete();
        }
    }
}
