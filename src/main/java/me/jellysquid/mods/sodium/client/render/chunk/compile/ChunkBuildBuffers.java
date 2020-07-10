package me.jellysquid.mods.sodium.client.render.chunk.compile;

import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.buffer.VertexData;
import me.jellysquid.mods.sodium.client.gl.util.BufferSlice;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.model.quad.sink.ModelQuadSinkDelegate;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPassManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.GlAllocationUtils;
import net.minecraft.client.util.math.Vector3d;
import net.minecraft.util.math.BlockPos;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A collection of temporary buffers for each worker thread which will be used to build chunk meshes for given render
 * passes. This makes a best-effort attempt to pick a suitable size for each scratch buffer, but will never try to
 * shrink a buffer.
 */
public class ChunkBuildBuffers {
    private final ChunkBuildBufferDelegate[] delegates;
    private final ChunkMeshBuilder[][] buildersByLayer;
    private final List<ChunkMeshBuilder> builders;
    private final GlVertexFormat<?> format;

    private final BlockRenderPassManager renderPassManager;

    public ChunkBuildBuffers(GlVertexFormat<?> format, BlockRenderPassManager renderPassManager) {
        this.format = format;
        this.renderPassManager = renderPassManager;

        this.delegates = new ChunkBuildBufferDelegate[BlockRenderPass.COUNT];
        this.buildersByLayer = new ChunkMeshBuilder[BlockRenderPass.COUNT][ModelQuadFacing.COUNT];

        for (RenderLayer layer : RenderLayer.getBlockLayers()) {
            int passId = this.renderPassManager.getRenderPassId(layer);

            for (ModelQuadFacing facing : ModelQuadFacing.VALUES) {
                this.buildersByLayer[passId][facing.ordinal()] =
                        new ChunkMeshBuilder(format, layer.getExpectedBufferSize() / ModelQuadFacing.COUNT);
            }

            this.delegates[passId] = new ChunkBuildBufferDelegate(this.buildersByLayer[passId]);
        }

        this.builders = Arrays.stream(this.buildersByLayer)
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
    }

    /**
     * Return the {@link ChunkMeshBuilder} for the given {@link RenderLayer} as mapped by the
     * {@link BlockRenderPassManager} for this render context.
     */
    public ChunkBuildBufferDelegate get(RenderLayer layer) {
        return this.delegates[this.renderPassManager.getRenderPassId(layer)];
    }

    /**
     * Creates immutable baked chunk meshes from all non-empty scratch buffers and resets the state of all mesh
     * builders. This is used after all blocks have been rendered to pass the finished meshes over to the graphics card.
     */
    public ChunkMeshData createMesh(Vector3d camera, BlockPos pos, BlockRenderPass pass) {
        ChunkMeshBuilder[] builders = this.buildersByLayer[pass.ordinal()];

        ChunkMeshData meshData = new ChunkMeshData();
        int bufferLen = 0;

        for (int facingId = 0; facingId < builders.length; facingId++) {
            ChunkMeshBuilder builder = builders[facingId];

            if (builder == null) {
                continue;
            }

            builder.finish();

            if (builder.isEmpty()) {
                continue;
            }

            ModelQuadFacing facing = ModelQuadFacing.VALUES[facingId];

            if (pass.isTranslucent()) {
                builder.sortQuads((float) camera.x - (float) pos.getX(),
                        (float) camera.y - (float) pos.getY(),
                        (float) camera.z - (float) pos.getZ());
            }

            int start = bufferLen;
            int size = builder.getSize();

            meshData.setModelSlice(facing, new BufferSlice(start, size));

            bufferLen += size;
        }

        if (bufferLen <= 0) {
            return null;
        }

        ByteBuffer buffer = GlAllocationUtils.allocateByteBuffer(bufferLen);

        for (Map.Entry<ModelQuadFacing, BufferSlice> entry : meshData.getSlices()) {
            BufferSlice slice = entry.getValue();
            buffer.position(slice.start);

            ChunkMeshBuilder builder = this.buildersByLayer[pass.ordinal()][entry.getKey().ordinal()];
            builder.copyInto(buffer);
        }

        buffer.flip();

        meshData.setVertexData(new VertexData(buffer, this.format));

        return meshData;
    }

    public void init(ChunkRenderData.Builder renderData) {
        for (ChunkMeshBuilder builder : this.builders) {
            builder.begin(renderData);
        }
    }

    public static class ChunkBuildBufferDelegate implements ModelQuadSinkDelegate {
        private final ChunkMeshBuilder[] builders;

        private ChunkBuildBufferDelegate(ChunkMeshBuilder[] builders) {
            this.builders = builders;
        }

        @Override
        public ChunkMeshBuilder get(ModelQuadFacing facing) {
            return this.builders[facing.ordinal()];
        }

        public void setOffset(int x, int y, int z) {
            for (ChunkMeshBuilder builder : this.builders) {
                builder.setOffset(x, y, z);
            }
        }
    }
}
