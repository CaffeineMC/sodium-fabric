package me.jellysquid.mods.sodium.render.chunk.compile.tasks;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.render.buffer.IndexedVertexData;
import me.jellysquid.mods.sodium.render.buffer.ElementRange;
import me.jellysquid.mods.sodium.render.terrain.quad.properties.ChunkMeshFace;
import me.jellysquid.mods.sodium.render.vertex.buffer.VertexBufferBuilder;
import me.jellysquid.mods.sodium.render.terrain.format.TerrainVertexType;
import me.jellysquid.mods.sodium.render.chunk.compile.buffers.DefaultChunkMeshBuilder;
import me.jellysquid.mods.sodium.render.chunk.compile.buffers.ChunkMeshBuilder;
import me.jellysquid.mods.sodium.render.chunk.compile.buffers.IndexBufferBuilder;
import me.jellysquid.mods.sodium.render.chunk.state.ChunkMesh;
import me.jellysquid.mods.sodium.render.chunk.state.ChunkRenderData;
import me.jellysquid.mods.sodium.render.terrain.format.TerrainVertexSink;
import me.jellysquid.mods.sodium.render.chunk.passes.ChunkRenderPass;
import me.jellysquid.mods.sodium.render.chunk.passes.ChunkRenderPassManager;
import me.jellysquid.mods.sodium.util.NativeBuffer;
import net.minecraft.client.render.RenderLayer;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * A collection of temporary buffers for each worker thread which will be used to build chunk meshes for given render
 * passes. This makes a best-effort attempt to pick a suitable size for each scratch buffer, but will never try to
 * shrink a buffer.
 */
public class TerrainBuildBuffers {
    private final Map<ChunkRenderPass, ChunkMeshBuilder> delegates;

    private final Map<ChunkRenderPass, VertexBufferBuilder> vertexBuffers;
    private final Map<ChunkRenderPass, IndexBufferBuilder[]> indexBuffers;

    private final TerrainVertexType vertexType;

    private final ChunkRenderPassManager renderPassManager;

    public TerrainBuildBuffers(TerrainVertexType vertexType, ChunkRenderPassManager renderPassManager) {
        this.vertexType = vertexType;
        this.renderPassManager = renderPassManager;

        this.delegates = new Reference2ReferenceOpenHashMap<>();

        this.vertexBuffers = new Reference2ReferenceOpenHashMap<>();
        this.indexBuffers = new Reference2ReferenceOpenHashMap<>();

        for (var renderPass : renderPassManager.getAllRenderPasses()) {
            IndexBufferBuilder[] indexBuffers = new IndexBufferBuilder[ChunkMeshFace.COUNT];

            for (int facing = 0; facing < ChunkMeshFace.COUNT; facing++) {
                indexBuffers[facing] = new IndexBufferBuilder(1024);
            }

            this.indexBuffers.put(renderPass, indexBuffers);
            this.vertexBuffers.put(renderPass, new VertexBufferBuilder(this.vertexType.getBufferVertexFormat(),
                    2 * 1024 * 1024));
        }
    }

    public void init(ChunkRenderData.Builder renderData, int chunkId) {
        for (VertexBufferBuilder vertexBuffer : this.vertexBuffers.values()) {
            vertexBuffer.start();
        }

        for (IndexBufferBuilder[] indexBuffers : this.indexBuffers.values()) {
            for (IndexBufferBuilder indexBuffer : indexBuffers) {
                indexBuffer.start();
            }
        }

        for (var renderPass : this.renderPassManager.getAllRenderPasses()) {
            TerrainVertexSink vertexSink = this.vertexType.createBufferWriter(this.vertexBuffers.get(renderPass));
            IndexBufferBuilder[] indexBuffers = this.indexBuffers.get(renderPass);

            this.delegates.put(renderPass, new DefaultChunkMeshBuilder(indexBuffers, vertexSink, renderData, chunkId));
        }
    }

    /**
     * Return the {@link ChunkMeshBuilder} for the given {@link RenderLayer} as mapped by the
     * {@link ChunkRenderPassManager} for this render context.
     */
    public ChunkMeshBuilder get(RenderLayer layer) {
        return this.delegates.get(this.renderPassManager.getRenderPassForLayer(layer));
    }

    public Map<ChunkRenderPass, ChunkMesh> createMeshes() {
        var map = new Reference2ReferenceOpenHashMap<ChunkRenderPass, ChunkMesh>();

        for (var renderPass : this.renderPassManager.getAllRenderPasses()) {
            var mesh = this.createMesh(renderPass);

            if (mesh != null) {
                map.put(renderPass, mesh);
            }
        }

        return map;
    }

    /**
     * Creates immutable baked chunk meshes from all non-empty scratch buffers. This is used after all blocks
     * have been rendered to pass the finished meshes over to the graphics card. This function can be called multiple
     * times to return multiple copies.
     */
    private ChunkMesh createMesh(ChunkRenderPass pass) {
        var vertexBufferBuilder = this.vertexBuffers.get(pass);
        var vertexBuffer = vertexBufferBuilder.pop();

        if (vertexBuffer == null) {
            return null;
        }

        var indexBufferBuilders = this.indexBuffers.get(pass);
        IndexBufferBuilder.Result[] indexBuffers = Arrays.stream(indexBufferBuilders)
                .map(IndexBufferBuilder::pop)
                .toArray(IndexBufferBuilder.Result[]::new);

        NativeBuffer indexBuffer = new NativeBuffer(Arrays.stream(indexBuffers)
                .filter(Objects::nonNull)
                .mapToInt(IndexBufferBuilder.Result::getByteSize)
                .sum());

        int indexPointer = 0;

        Map<ChunkMeshFace, ElementRange> ranges = new EnumMap<>(ChunkMeshFace.class);

        for (ChunkMeshFace facing : ChunkMeshFace.VALUES) {
            IndexBufferBuilder.Result indices = indexBuffers[facing.ordinal()];

            if (indices == null) {
                continue;
            }

            ranges.put(facing,
                    new ElementRange(indexPointer, indices.getCount(), indices.getFormat(), indices.getBaseVertex()));

            indexPointer = indices.writeTo(indexPointer, indexBuffer.getDirectBuffer());
        }

        IndexedVertexData vertexData = new IndexedVertexData(this.vertexType.getCustomVertexFormat(),
                vertexBuffer, indexBuffer);

        return new ChunkMesh(vertexData, ranges);
    }

    public void destroy() {
        for (VertexBufferBuilder builder : this.vertexBuffers.values()) {
            builder.destroy();
        }
    }
}
