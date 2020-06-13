package me.jellysquid.mods.sodium.client.render.chunk.backends.gl20;

import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlMutableBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.VertexData;
import me.jellysquid.mods.sodium.client.gl.util.BufferSlice;
import me.jellysquid.mods.sodium.client.gl.util.VertexSlice;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import org.lwjgl.opengl.GL15;

public class VBOGraphicsState implements ChunkGraphicsState {
    private final GlBuffer buffer;
    private final long[] layers;

    public VBOGraphicsState() {
        this.buffer = new GlMutableBuffer(GL15.GL_STATIC_DRAW);
        this.layers = new long[BlockRenderPass.count()];
    }

    @Override
    public void delete() {
        this.buffer.delete();
    }

    public void upload(ChunkMeshData meshData) {
        VertexData data = meshData.takePendingUpload();

        this.buffer.bind(GL15.GL_ARRAY_BUFFER);
        this.buffer.upload(GL15.GL_ARRAY_BUFFER, data);
        this.buffer.unbind(GL15.GL_ARRAY_BUFFER);

        GlVertexFormat<?> vertexFormat = data.format;
        int stride = vertexFormat.getStride();

        for (BlockRenderPass pass : BlockRenderPass.VALUES) {
            BufferSlice slice = meshData.getSlice(pass);

            if (slice != null) {
                this.layers[pass.ordinal()] = VertexSlice.pack(slice.start / stride, slice.len / stride);
            }
        }
    }

    public long getSliceForLayer(BlockRenderPass pass) {
        return this.layers[pass.ordinal()];
    }

    public GlBuffer getBuffer() {
        return this.buffer;
    }
}
