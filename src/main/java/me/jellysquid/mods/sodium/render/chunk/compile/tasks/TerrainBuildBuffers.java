package me.jellysquid.mods.sodium.render.chunk.compile.tasks;

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
    private final ChunkMeshBuilder[] delegates;

    private final VertexBufferBuilder[] vertexBuffers;
    private final IndexBufferBuilder[][] indexBuffers;

    private final TerrainVertexType vertexType;

    private final ChunkRenderPassManager renderPassManager;

    public TerrainBuildBuffers(TerrainVertexType vertexType, ChunkRenderPassManager renderPassManager) {
        this.vertexType = vertexType;
        this.renderPassManager = renderPassManager;

        this.delegates = new ChunkMeshBuilder[ChunkRenderPass.COUNT];

        this.vertexBuffers = new VertexBufferBuilder[ChunkRenderPass.COUNT];
        this.indexBuffers = new IndexBufferBuilder[ChunkRenderPass.COUNT][ChunkMeshFace.COUNT];

        for (ChunkRenderPass pass : ChunkRenderPass.VALUES) {
            IndexBufferBuilder[] indexBuffers = this.indexBuffers[pass.ordinal()];

            for (int facing = 0; facing < ChunkMeshFace.COUNT; facing++) {
                indexBuffers[facing] = new IndexBufferBuilder(1024);
            }

            this.vertexBuffers[pass.ordinal()] = new VertexBufferBuilder(this.vertexType.getBufferVertexFormat(),
                    pass.getLayer().getExpectedBufferSize());
        }
    }

    public void init(ChunkRenderData.Builder renderData, int chunkId) {
        for (VertexBufferBuilder vertexBuffer : this.vertexBuffers) {
            vertexBuffer.start();
        }

        for (IndexBufferBuilder[] indexBuffers : this.indexBuffers) {
            for (IndexBufferBuilder indexBuffer : indexBuffers) {
                indexBuffer.start();
            }
        }

        for (int i = 0; i < this.delegates.length; i++) {
            TerrainVertexSink vertexSink = this.vertexType.createBufferWriter(this.vertexBuffers[i]);
            IndexBufferBuilder[] indexBuffers = this.indexBuffers[i];

            this.delegates[i] = new DefaultChunkMeshBuilder(indexBuffers, vertexSink, renderData, chunkId);
        }
    }

    /**
     * Return the {@link ChunkMeshBuilder} for the given {@link RenderLayer} as mapped by the
     * {@link ChunkRenderPassManager} for this render context.
     */
    public ChunkMeshBuilder get(RenderLayer layer) {
        return this.delegates[this.renderPassManager.getRenderPassId(layer)];
    }

    /**
     * Creates immutable baked chunk meshes from all non-empty scratch buffers. This is used after all blocks
     * have been rendered to pass the finished meshes over to the graphics card. This function can be called multiple
     * times to return multiple copies.
     */
    public ChunkMesh createMesh(ChunkRenderPass pass) {
        NativeBuffer vertexBuffer = this.vertexBuffers[pass.ordinal()].pop();

        if (vertexBuffer == null) {
            return null;
        }

        IndexBufferBuilder.Result[] indexBuffers = Arrays.stream(this.indexBuffers[pass.ordinal()])
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
        for (VertexBufferBuilder builder : this.vertexBuffers) {
            builder.destroy();
        }
    }
}
