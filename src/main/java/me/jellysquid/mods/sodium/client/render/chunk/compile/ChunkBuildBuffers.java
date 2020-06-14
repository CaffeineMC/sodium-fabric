package me.jellysquid.mods.sodium.client.render.chunk.compile;

import it.unimi.dsi.fastutil.bytes.Byte2ReferenceArrayMap;
import it.unimi.dsi.fastutil.bytes.Byte2ReferenceMap;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.buffer.VertexData;
import me.jellysquid.mods.sodium.client.model.ModelQuadSinkDelegate;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkModelPart;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkModelSlice;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPassManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.GlAllocationUtils;
import net.minecraft.client.util.math.Vector3d;
import net.minecraft.util.math.BlockPos;

import java.nio.ByteBuffer;

/**
 * A collection of temporary buffers for each worker thread which will be used to build chunk meshes for given render
 * passes. This makes a best-effort attempt to pick a suitable size for each scratch buffer, but will never try to
 * shrink a buffer.
 */
public class ChunkBuildBuffers {
    private final ChunkBuildBufferDelegate[] delegates;
    private final ChunkMeshBuilder[][] builders;
    private final GlVertexFormat<?> format;

    private final BlockRenderPassManager renderPassManager;

    public ChunkBuildBuffers(GlVertexFormat<?> format, BlockRenderPassManager renderPassManager) {
        this.format = format;
        this.renderPassManager = renderPassManager;

        this.delegates = new ChunkBuildBufferDelegate[BlockRenderPass.COUNT];
        this.builders = new ChunkMeshBuilder[BlockRenderPass.COUNT][ModelQuadFacing.COUNT];

        for (RenderLayer layer : RenderLayer.getBlockLayers()) {
            int passId = this.renderPassManager.getRenderPassId(layer);

            for (ModelQuadFacing facing : ModelQuadFacing.VALUES) {
                this.builders[passId][facing.ordinal()] =
                        new ChunkMeshBuilder(format, layer.getExpectedBufferSize() / ModelQuadFacing.COUNT);
            }

            this.delegates[passId] = new ChunkBuildBufferDelegate(this.builders[passId]);
        }
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
    public ChunkMeshData createMeshes(Vector3d camera, BlockPos pos) {
        Byte2ReferenceMap<ChunkModelSlice> staging = new Byte2ReferenceArrayMap<>();

        int bufferLen = 0;

        for (int passId = 0; passId < this.builders.length; passId++) {
            ChunkMeshBuilder[] builders = this.builders[passId];

            for (int facingId = 0; facingId < builders.length; facingId++) {
                ChunkMeshBuilder builder = builders[facingId];

                if (builder == null || builder.isEmpty()) {
                    continue;
                }

                BlockRenderPass pass = this.renderPassManager.getRenderPass(passId);
                ModelQuadFacing facing = ModelQuadFacing.VALUES[facingId];

                if (pass.isTranslucent()) {
                    builder.sortQuads((float) camera.x - (float) pos.getX(),
                            (float) camera.y - (float) pos.getY(),
                            (float) camera.z - (float) pos.getZ());
                }

                int start = bufferLen;
                int size = builder.getSize();

                staging.put(ChunkModelPart.encodeKey(pass, facing), new ChunkModelSlice(start, size, builder));

                bufferLen += size;
            }
        }

        if (bufferLen <= 0) {
            return ChunkMeshData.EMPTY;
        }

        ByteBuffer buffer = GlAllocationUtils.allocateByteBuffer(bufferLen);

        for (Byte2ReferenceMap.Entry<ChunkModelSlice> entry : staging.byte2ReferenceEntrySet()) {
            ChunkModelSlice slice = entry.getValue();
            buffer.position(slice.start);

            slice.builder.copyInto(buffer);
        }

        buffer.flip();

        return new ChunkMeshData(new VertexData(buffer, this.format), staging);
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
