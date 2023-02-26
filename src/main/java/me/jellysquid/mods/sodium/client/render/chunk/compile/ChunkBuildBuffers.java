package me.jellysquid.mods.sodium.client.render.chunk.compile;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import me.jellysquid.mods.sodium.client.gl.buffer.IndexedVertexData;
import me.jellysquid.mods.sodium.client.gl.util.ElementRange;
import me.jellysquid.mods.sodium.client.model.IndexBufferBuilder;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.vertex.type.ChunkVertexBufferBuilder;
import me.jellysquid.mods.sodium.client.render.vertex.type.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.BakedChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPassManager;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import net.minecraft.client.render.RenderLayer;
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
    private final ChunkModelBuilder[] delegates;

    private final ChunkVertexBufferBuilder[] vertexBuffers;
    private final IndexBufferBuilder[][] indexBuffers;

    private final ChunkVertexType vertexType;

    private final BlockRenderPassManager renderPassManager;

    public ChunkBuildBuffers(ChunkVertexType vertexType, BlockRenderPassManager renderPassManager) {
        this.vertexType = vertexType;
        this.renderPassManager = renderPassManager;

        this.delegates = new ChunkModelBuilder[BlockRenderPass.COUNT];

        this.vertexBuffers = new ChunkVertexBufferBuilder[BlockRenderPass.COUNT];
        this.indexBuffers = new IndexBufferBuilder[BlockRenderPass.COUNT][ModelQuadFacing.COUNT];

        for (BlockRenderPass pass : BlockRenderPass.VALUES) {
            IndexBufferBuilder[] indexBuffers = this.indexBuffers[pass.ordinal()];

            for (int facing = 0; facing < ModelQuadFacing.COUNT; facing++) {
                indexBuffers[facing] = new IndexBufferBuilder(1024);
            }

            this.vertexBuffers[pass.ordinal()] = new ChunkVertexBufferBuilder(this.vertexType, pass.getLayer().getExpectedBufferSize());
        }
    }

    public void init(ChunkRenderData.Builder renderData, int chunkId) {
        for (ChunkVertexBufferBuilder vertexBuffer : this.vertexBuffers) {
            vertexBuffer.start(chunkId);
        }

        for (IndexBufferBuilder[] indexBuffers : this.indexBuffers) {
            for (IndexBufferBuilder indexBuffer : indexBuffers) {
                indexBuffer.start();
            }
        }

        for (int i = 0; i < this.delegates.length; i++) {
            this.delegates[i] = new BakedChunkModelBuilder(this.vertexBuffers[i], this.indexBuffers[i], renderData);
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
     * Creates immutable baked chunk meshes from all non-empty scratch buffers. This is used after all blocks
     * have been rendered to pass the finished meshes over to the graphics card. This function can be called multiple
     * times to return multiple copies.
     */
    public ChunkMeshData createMesh(BlockRenderPass pass) {
        NativeBuffer vertexBuffer = this.vertexBuffers[pass.ordinal()].pop();

        if (vertexBuffer == null) {
            return null;
        }

        IntArrayList[] indexBuffers = Arrays.stream(this.indexBuffers[pass.ordinal()])
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
        for (ChunkVertexBufferBuilder builder : this.vertexBuffers) {
            builder.destroy();
        }
    }
}
