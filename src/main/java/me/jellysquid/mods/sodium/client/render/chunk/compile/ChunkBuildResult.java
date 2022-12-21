package me.jellysquid.mods.sodium.client.render.chunk.compile;

import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;

import java.util.Map;

/**
 * The result of a chunk rebuild task which contains any and all data that needs to be processed or uploaded on
 * the main thread. If a task is cancelled after finishing its work and not before the result is processed, the result
 * will instead be discarded.
 */
public class ChunkBuildResult {
    public final RenderSection section;
    public final ChunkRenderData data;
    public final Map<BlockRenderPass, ChunkMeshData> meshes;
    public final int timestamp;

    public ChunkBuildResult(RenderSection section, ChunkRenderData data, Map<BlockRenderPass, ChunkMeshData> meshes, int timestamp) {
        this.section = section;
        this.data = data;
        this.meshes = meshes;
        this.timestamp = timestamp;
    }

    public ChunkMeshData getMesh(BlockRenderPass pass) {
        return this.meshes.get(pass);
    }

    public void delete() {
        for (ChunkMeshData data : this.meshes.values()) {
            data.getVertexData()
                    .delete();
        }
    }
}
