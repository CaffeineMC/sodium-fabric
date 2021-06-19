package me.jellysquid.mods.sodium.client.render.chunk.compile;

import me.jellysquid.mods.sodium.client.gl.buffer.IndexedVertexData;
import me.jellysquid.mods.sodium.client.gl.util.ElementRange;
import me.jellysquid.mods.sodium.client.model.IndexBufferBuilder;
import me.jellysquid.mods.sodium.client.model.PrimitiveSink;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferBuilder;
import me.jellysquid.mods.sodium.client.model.vertex.type.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.BakedChunkModelBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelVertexTransformer;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.format.ChunkModelOffset;
import me.jellysquid.mods.sodium.client.render.chunk.format.ModelVertexSink;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPassManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.GlAllocationUtils;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * A collection of temporary buffers for each worker thread which will be used to build chunk meshes for given render
 * passes. This makes a best-effort attempt to pick a suitable size for each scratch buffer, but will never try to
 * shrink a buffer.
 */
public class ChunkBuildBuffers {
    private final ChunkModelBuffers[] delegates;

    private final VertexBufferBuilder[][] vertexBuffers;
    private final IndexBufferBuilder[][] indexBuffers;

    private final ChunkVertexType vertexType;

    private final BlockRenderPassManager renderPassManager;
    private final ChunkModelOffset offset;

    public ChunkBuildBuffers(ChunkVertexType vertexType, BlockRenderPassManager renderPassManager) {
        this.vertexType = vertexType;
        this.renderPassManager = renderPassManager;

        this.delegates = new ChunkModelBuffers[BlockRenderPass.COUNT];

        this.vertexBuffers = new VertexBufferBuilder[BlockRenderPass.COUNT][ModelQuadFacing.COUNT];
        this.indexBuffers = new IndexBufferBuilder[BlockRenderPass.COUNT][ModelQuadFacing.COUNT];

        this.offset = new ChunkModelOffset();

        for (BlockRenderPass pass : BlockRenderPass.VALUES) {
            VertexBufferBuilder[] vertexBuffers = this.vertexBuffers[pass.ordinal()];
            IndexBufferBuilder[] indexBuffers = this.indexBuffers[pass.ordinal()];

            int vertexBufferSize = pass.getLayer().getExpectedBufferSize() / ModelQuadFacing.COUNT;

            for (int facing = 0; facing < ModelQuadFacing.COUNT; facing++) {
                indexBuffers[facing] = new IndexBufferBuilder(1024);
                vertexBuffers[facing] = new VertexBufferBuilder(this.vertexType.getBufferVertexFormat(), vertexBufferSize);
            }
        }
    }

    public void init(ChunkRenderData.Builder renderData) {
        for (int layer = 0; layer < this.indexBuffers.length; layer++) {
            // TODO: Fix unsafe cast
            PrimitiveSink<ModelVertexSink>[] writers = new PrimitiveSink[ModelQuadFacing.COUNT];

            for (int facing = 0; facing < ModelQuadFacing.COUNT; facing++) {
                writers[facing] = new PrimitiveSink<>(this.indexBuffers[layer][facing],
                        new ChunkModelVertexTransformer(this.vertexType.createBufferWriter(this.vertexBuffers[layer][facing]), this.offset));
            }

            this.delegates[layer] = new BakedChunkModelBuffers(writers, renderData);
        }
    }

    /**
     * Return the {@link ChunkModelVertexTransformer} for the given {@link RenderLayer} as mapped by the
     * {@link BlockRenderPassManager} for this render context.
     */
    public ChunkModelBuffers get(RenderLayer layer) {
        return this.delegates[this.renderPassManager.getRenderPassId(layer)];
    }

    /**
     * Creates immutable baked chunk meshes from all non-empty scratch buffers and resets the state of all mesh
     * builders. This is used after all blocks have been rendered to pass the finished meshes over to the graphics card.
     */
    public ChunkMeshData createMesh(BlockRenderPass pass) {
        ChunkMeshData meshData = new ChunkMeshData();

        VertexBufferBuilder[] vertexBufferBuilders = this.vertexBuffers[pass.ordinal()];
        IndexBufferBuilder[] indexBufferBuilders = this.indexBuffers[pass.ordinal()];

        int vertexStride = this.vertexType.getBufferVertexFormat().getStride();

        int vertexDataLength = Arrays.stream(vertexBufferBuilders)
                .mapToInt(VertexBufferBuilder::getSize)
                .sum();

        int indexDataLength = Arrays.stream(indexBufferBuilders)
                .mapToInt(IndexBufferBuilder::getSize)
                .sum();

        ByteBuffer vertexBuffer = GlAllocationUtils.allocateByteBuffer(vertexDataLength);
        ByteBuffer indexBuffer = GlAllocationUtils.allocateByteBuffer(indexDataLength);

        int baseIndex = 0;
        int baseVertex = 0;

        for (ModelQuadFacing facing : ModelQuadFacing.VALUES) {
            IndexBufferBuilder indexBufferBuilder = indexBufferBuilders[facing.ordinal()];
            VertexBufferBuilder vertexBufferBuilder = vertexBufferBuilders[facing.ordinal()];

            if (indexBufferBuilder.getCount() == 0) {
                continue;
            }

            int indexCount = indexBufferBuilder.getCount();
            int vertexCount = vertexBufferBuilder.getSize() / vertexStride;

            meshData.setModelSlice(facing, new ElementRange(baseIndex, indexCount, baseVertex));

            vertexBufferBuilder.get(vertexBuffer);
            vertexBufferBuilder.reset();

            indexBufferBuilder.get(indexBuffer);
            indexBufferBuilder.reset();

            baseIndex += indexCount;
            baseVertex += vertexCount;
        }

        vertexBuffer.flip();
        indexBuffer.flip();

        IndexedVertexData vertexData = new IndexedVertexData(this.vertexType.getCustomVertexFormat(), vertexBuffer, indexBuffer);

        meshData.setVertexData(vertexData);

        return meshData;
    }

    public void setRenderOffset(int x, int y, int z) {
        this.offset.set(x, y, z);
    }
}
