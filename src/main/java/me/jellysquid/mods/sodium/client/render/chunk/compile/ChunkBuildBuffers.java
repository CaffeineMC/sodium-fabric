package me.jellysquid.mods.sodium.client.render.chunk.compile;

import me.jellysquid.mods.sodium.client.gl.buffer.IndexedVertexData;
import me.jellysquid.mods.sodium.client.gl.util.ElementRange;
import me.jellysquid.mods.sodium.client.model.IndexBufferBuilder;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferBuilder;
import me.jellysquid.mods.sodium.client.model.vertex.type.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.BakedChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.format.ModelVertexSink;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPassManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.GlAllocationUtils;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

/**
 * A collection of temporary buffers for each worker thread which will be used to build chunk meshes for given render
 * passes. This makes a best-effort attempt to pick a suitable size for each scratch buffer, but will never try to
 * shrink a buffer.
 */
public class ChunkBuildBuffers {
    private final ChunkModelBuilder[] delegates;

    private final VertexBufferBuilder[] vertexBuffers;
    private final IndexBufferBuilder[][] indexBuffers;

    private final ChunkVertexType vertexType;

    private final BlockRenderPassManager renderPassManager;

    public ChunkBuildBuffers(ChunkVertexType vertexType, BlockRenderPassManager renderPassManager) {
        this.vertexType = vertexType;
        this.renderPassManager = renderPassManager;

        this.delegates = new ChunkModelBuilder[BlockRenderPass.COUNT];

        this.vertexBuffers = new VertexBufferBuilder[BlockRenderPass.COUNT];
        this.indexBuffers = new IndexBufferBuilder[BlockRenderPass.COUNT][ModelQuadFacing.COUNT];

        for (BlockRenderPass pass : BlockRenderPass.VALUES) {
            IndexBufferBuilder[] indexBuffers = this.indexBuffers[pass.ordinal()];

            for (int facing = 0; facing < ModelQuadFacing.COUNT; facing++) {
                indexBuffers[facing] = new IndexBufferBuilder(1024);
            }

            this.vertexBuffers[pass.ordinal()] = new VertexBufferBuilder(this.vertexType.getBufferVertexFormat(),
                    pass.getLayer().getExpectedBufferSize());
        }
    }

    public void init(ChunkRenderData.Builder renderData) {
        for (int i = 0; i < this.delegates.length; i++) {
            ModelVertexSink vertexSink = this.vertexType.createBufferWriter(this.vertexBuffers[i]);
            IndexBufferBuilder[] indexBuffers = this.indexBuffers[i];

            this.delegates[i] = new BakedChunkModelBuilder(indexBuffers, vertexSink, renderData);
        }
    }

    /**
     * Return the {@link ChunkModelBuilder} for the given {@link RenderLayer} as mapped by the
     * {@link BlockRenderPassManager} for this render context.
     */
    public ChunkModelBuilder get(RenderLayer layer) {
        return this.delegates[this.renderPassManager.getRenderPassId(layer)];
    }

    /**
     * Creates immutable baked chunk meshes from all non-empty scratch buffers and resets the state of all mesh
     * builders. This is used after all blocks have been rendered to pass the finished meshes over to the graphics card.
     */
    public ChunkMeshData createMesh(BlockRenderPass pass) {
        VertexBufferBuilder vertexBufferBuilder = this.vertexBuffers[pass.ordinal()];
        IndexBufferBuilder[] indexBufferBuilders = this.indexBuffers[pass.ordinal()];

        int vertexDataLength = vertexBufferBuilder.getByteSize();

        if (vertexDataLength == 0) {
            return null;
        }

        int indexDataLength = Arrays.stream(indexBufferBuilders)
                .mapToInt(IndexBufferBuilder::getSize)
                .sum();

        ByteBuffer vertexBuffer = GlAllocationUtils.allocateByteBuffer(vertexDataLength);
        ByteBuffer indexBuffer = GlAllocationUtils.allocateByteBuffer(indexDataLength);

        int baseIndex = 0;

        Map<ModelQuadFacing, ElementRange> ranges = new EnumMap<>(ModelQuadFacing.class);

        for (ModelQuadFacing facing : ModelQuadFacing.VALUES) {
            IndexBufferBuilder indexBufferBuilder = indexBufferBuilders[facing.ordinal()];

            if (indexBufferBuilder.getCount() == 0) {
                continue;
            }

            int indexCount = indexBufferBuilder.getCount();

            ranges.put(facing, new ElementRange(baseIndex, indexCount));

            indexBufferBuilder.get(indexBuffer);
            indexBufferBuilder.reset();

            baseIndex += indexCount;
        }

        vertexBufferBuilder.get(vertexBuffer);
        vertexBufferBuilder.reset();

        vertexBuffer.flip();
        indexBuffer.flip();

        IndexedVertexData vertexData = new IndexedVertexData(this.vertexType.getCustomVertexFormat(),
                vertexBuffer, indexBuffer);

        return new ChunkMeshData(vertexData, ranges);
    }
}
