package me.jellysquid.mods.sodium.client.render.chunk.compile;

import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.buffer.VertexData;
import me.jellysquid.mods.sodium.client.gl.util.BufferSlice;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.model.quad.sink.ModelQuadSinkDelegate;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockLayer;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.GlAllocationUtils;

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
    private final ChunkMeshBuilder[][] buildersByPass;
    private final List<ChunkMeshBuilder> builders;
    private final GlVertexFormat<?> format;

    public ChunkBuildBuffers(GlVertexFormat<?> format) {
        this.format = format;

        this.delegates = new ChunkBuildBufferDelegate[BlockLayer.COUNT];
        this.buildersByPass = new ChunkMeshBuilder[BlockLayer.COUNT][ModelQuadFacing.COUNT];

        for (BlockLayer layer : BlockLayer.VALUES) {
            for (ModelQuadFacing facing : ModelQuadFacing.VALUES) {
                this.buildersByPass[layer.ordinal()][facing.ordinal()] =
                        new ChunkMeshBuilder(format, layer, layer.getExpectedSize() / ModelQuadFacing.COUNT);
            }

            this.delegates[layer.ordinal()] = new ChunkBuildBufferDelegate(this.buildersByPass[layer.ordinal()]);
        }

        this.builders = Arrays.stream(this.buildersByPass)
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
    }

    public ChunkBuildBufferDelegate get(RenderLayer layer) {
        return this.delegates[BlockLayer.fromRenderLayer(layer)];
    }

    /**
     * Creates immutable baked chunk meshes from all non-empty scratch buffers and resets the state of all mesh
     * builders. This is used after all blocks have been rendered to pass the finished meshes over to the graphics card.
     */
    public ChunkMeshData createMesh(BlockRenderPass pass) {
        ChunkMeshData meshData = new ChunkMeshData();
        int meshSize = 0;

        for (ModelQuadFacing facing : ModelQuadFacing.VALUES) {
            int facingSize = 0;

            for (BlockLayer layer : pass.getLayers()) {
                ChunkMeshBuilder builder = this.buildersByPass[layer.ordinal()][facing.ordinal()];
                builder.finish();

                facingSize += builder.getSize();
            }

            meshData.setModelSlice(facing, new BufferSlice(meshSize, facingSize));
            meshSize += facingSize;
        }

        if (meshSize <= 0) {
            return null;
        }

        ByteBuffer buffer = GlAllocationUtils.allocateByteBuffer(meshSize);

        for (Map.Entry<ModelQuadFacing, BufferSlice> entry : meshData.getSlices()) {
            ModelQuadFacing key = entry.getKey();

            for (BlockLayer layer : pass.getLayers()) {
                ChunkMeshBuilder builder = this.buildersByPass[layer.ordinal()][key.ordinal()];

                if (!builder.isEmpty()) {
                    builder.copyInto(buffer);
                }
            }
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
