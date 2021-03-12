package me.jellysquid.mods.sodium.client.render.chunk.compile;

import me.jellysquid.mods.sodium.client.gl.buffer.VertexData;
import me.jellysquid.mods.sodium.client.gl.util.BufferSlice;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferBuilder;
import me.jellysquid.mods.sodium.client.model.vertex.type.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.BakedChunkModelBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelVertexTransformer;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.format.ChunkModelOffset;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPassManager;
import me.jellysquid.mods.sodium.client.util.UnsafeUtil;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.GlAllocationUtils;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * A collection of temporary buffers for each worker thread which will be used to build chunk meshes for given render
 * passes. This makes a best-effort attempt to pick a suitable size for each scratch buffer, but will never try to
 * shrink a buffer.
 */
public class ChunkBuildBuffers {
    private final ChunkModelBuffers[] delegates;
    private final VertexBufferBuilder[][] buffersByLayer;
    private final ChunkVertexType vertexType;

    private final BlockRenderPassManager renderPassManager;
    private final ChunkModelOffset offset;

    public ChunkBuildBuffers(ChunkVertexType vertexType, BlockRenderPassManager renderPassManager) {
        this.vertexType = vertexType;
        this.renderPassManager = renderPassManager;

        this.delegates = new ChunkModelBuffers[BlockRenderPass.COUNT];
        this.buffersByLayer = new VertexBufferBuilder[BlockRenderPass.COUNT][ModelQuadFacing.COUNT];

        this.offset = new ChunkModelOffset();

        for (RenderLayer layer : RenderLayer.getBlockLayers()) {
            int passId = this.renderPassManager.getRenderPassId(layer);

            VertexBufferBuilder[] buffers = this.buffersByLayer[passId];

            for (ModelQuadFacing facing : ModelQuadFacing.VALUES) {
                buffers[facing.ordinal()] = new VertexBufferBuilder(vertexType.getBufferVertexFormat(), layer.getExpectedBufferSize() / ModelQuadFacing.COUNT);
            }
        }
    }

    public void init(ChunkRenderData.Builder renderData) {
        for (int i = 0; i < this.buffersByLayer.length; i++) {
            ChunkModelVertexTransformer[] writers = new ChunkModelVertexTransformer[ModelQuadFacing.COUNT];

            for (ModelQuadFacing facing : ModelQuadFacing.VALUES) {
                writers[facing.ordinal()] = new ChunkModelVertexTransformer(this.vertexType.createBufferWriter(this.buffersByLayer[i][facing.ordinal()], UnsafeUtil.isAvailable()), this.offset);
            }

            this.delegates[i] = new BakedChunkModelBuffers(writers, renderData);
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
        VertexBufferBuilder[] builders = this.buffersByLayer[pass.ordinal()];

        ChunkMeshData meshData = new ChunkMeshData();
        int bufferLen = 0;

        for (int facingId = 0; facingId < builders.length; facingId++) {
            VertexBufferBuilder builder = builders[facingId];

            if (builder == null || builder.isEmpty()) {
                continue;
            }

            int start = bufferLen;
            int size = builder.getSize();

            meshData.setModelSlice(ModelQuadFacing.VALUES[facingId], new BufferSlice(start, size));

            bufferLen += size;
        }

        if (bufferLen <= 0) {
            return null;
        }

        ByteBuffer buffer = GlAllocationUtils.allocateByteBuffer(bufferLen);

        for (Map.Entry<ModelQuadFacing, BufferSlice> entry : meshData.getSlices()) {
            BufferSlice slice = entry.getValue();
            buffer.position(slice.start);

            VertexBufferBuilder builder = this.buffersByLayer[pass.ordinal()][entry.getKey().ordinal()];
            builder.copyInto(buffer);
        }

        buffer.flip();

        meshData.setVertexData(new VertexData(buffer, this.vertexType.getCustomVertexFormat()));

        return meshData;
    }

    public void setRenderOffset(int x, int y, int z) {
        this.offset.set(x, y, z);
    }
}
