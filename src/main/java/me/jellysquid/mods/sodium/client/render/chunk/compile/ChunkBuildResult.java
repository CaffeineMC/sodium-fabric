package me.jellysquid.mods.sodium.client.render.chunk.compile;

import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.format.ChunkMesh;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderLayer;
import me.jellysquid.mods.sodium.client.render.chunk.passes.ChunkMeshType;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * The result of a chunk rebuild task which contains any and all data that needs to be processed or uploaded on
 * the main thread. If a task is cancelled after finishing its work and not before the result is processed, the result
 * will instead be discarded.
 */
public class ChunkBuildResult {
    public final RenderSection render;
    public final ChunkRenderData data;
    public final Map<BlockRenderLayer, Map<ChunkMeshType<?>, ChunkMesh>> meshes;
    public final int buildTime;

    public ChunkBuildResult(RenderSection render, ChunkRenderData data, Map<BlockRenderLayer, Map<ChunkMeshType<?>, ChunkMesh>> meshes, int buildTime) {
        this.render = render;
        this.data = data;
        this.meshes = meshes;
        this.buildTime = buildTime;
    }

    public Iterable<Map.Entry<BlockRenderLayer, Map<ChunkMeshType<?>, ChunkMesh>>> getMeshes() {
        return Collections.unmodifiableSet(this.meshes.entrySet());
    }

    @Deprecated
    public void delete() {
        for (var meshes : this.meshes.values()) {
            for (var mesh : meshes.values()) {
                mesh.delete();
            }
        }
    }
}
