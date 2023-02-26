package me.jellysquid.mods.sodium.client.render.chunk.compile;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.gl.buffer.IndexedVertexData;
import me.jellysquid.mods.sodium.client.gl.util.ElementRange;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import me.jellysquid.mods.sodium.client.model.IndexBufferBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.Material;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.BakedChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.builder.ChunkMeshBufferBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import org.lwjgl.system.MemoryUtil;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

/**
 * A collection of temporary buffers for each worker thread which will be used to build chunk meshes for given render
 * passes. This makes a best-effort attempt to pick a suitable size for each scratch buffer, but will never try to
 * shrink a buffer.
 */
public class ChunkBuildBuffers {
    private final Reference2ReferenceOpenHashMap<TerrainRenderPass, BakedChunkModelBuilder> builders = new Reference2ReferenceOpenHashMap<>();

    private final ChunkVertexType vertexType;

    public ChunkBuildBuffers(ChunkVertexType vertexType) {
        this.vertexType = vertexType;

        for (TerrainRenderPass pass : DefaultTerrainRenderPasses.ALL) {
            var vertexBuffer = new ChunkMeshBufferBuilder(this.vertexType, 2 * 1024 * 1024);
            var indexBuffers = new IndexBufferBuilder[ModelQuadFacing.COUNT];

            for (int facing = 0; facing < ModelQuadFacing.COUNT; facing++) {
                indexBuffers[facing] = new IndexBufferBuilder(1024 * 8);
            }

            this.builders.put(pass, new BakedChunkModelBuilder(vertexBuffer, indexBuffers));
        }
    }

    public void init(ChunkRenderData.Builder renderData, int chunkId) {
        for (var builder : this.builders.values()) {
            builder.begin(renderData, chunkId);
        }
    }

    public ChunkModelBuilder get(Material material) {
        return this.builders.get(material.pass);
    }

    /**
     * Creates immutable baked chunk meshes from all non-empty scratch buffers. This is used after all blocks
     * have been rendered to pass the finished meshes over to the graphics card. This function can be called multiple
     * times to return multiple copies.
     */
    public ChunkMeshData createMesh(TerrainRenderPass pass) {
        var builder = this.builders.get(pass);
        var vertexBuffer = builder.getVertexBuffer().pop();

        if (vertexBuffer == null) {
            return null;
        }

        IntArrayList[] indexBuffers = Arrays.stream(builder.getIndexBuffers())
                .map(IndexBufferBuilder::pop)
                .toArray(IntArrayList[]::new);

        NativeBuffer indexBuffer = new NativeBuffer(Arrays.stream(indexBuffers)
                .mapToInt((array) -> array.size() * Integer.BYTES)
                .sum());

        int indexPointer = 0;

        Map<ModelQuadFacing, ElementRange> ranges = new EnumMap<>(ModelQuadFacing.class);

        for (ModelQuadFacing facing : ModelQuadFacing.VALUES) {
            var indices = indexBuffers[facing.ordinal()];

            if (indices.isEmpty()) {
                continue;
            }

            ranges.put(facing, new ElementRange(indexPointer, indices.size()));

            copyToIntBuffer(MemoryUtil.memAddress(indexBuffer.getDirectBuffer(), indexPointer), indices);
            indexPointer += indices.size() * Integer.BYTES;
        }

        IndexedVertexData vertexData = new IndexedVertexData(this.vertexType.getVertexFormat(),
                vertexBuffer, indexBuffer);

        return new ChunkMeshData(vertexData, ranges);
    }

    private static void copyToIntBuffer(long ptr, IntArrayList list) {
        for (int i : list) {
            MemoryUtil.memPutInt(ptr, i);
            ptr += 4;
        }
    }

    public void destroy() {
        for (var builder : this.builders.values()) {
            builder.destroy();
        }
    }
}
