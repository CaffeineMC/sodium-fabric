package me.jellysquid.mods.sodium.client.render.chunk.backends.gl30;

import me.jellysquid.mods.sodium.client.gl.array.GlVertexArray;
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

public class VAOGraphicsState implements ChunkGraphicsState {
    private final GlVertexArray vertexArray;
    private GlBuffer vertexBuffer;

    private final long[] layers;

    public VAOGraphicsState() {
        this.vertexBuffer = new GlMutableBuffer(GL15.GL_STATIC_DRAW);
        this.vertexArray = new GlVertexArray();
        this.layers = new long[BlockRenderPass.count()];
    }

    @Override
    public void delete() {
        this.vertexBuffer.delete();
        this.vertexArray.delete();
    }

    public long getSliceForLayer(BlockRenderPass pass) {
        return this.layers[pass.ordinal()];
    }

    public GlVertexArray getVertexArray() {
        return this.vertexArray;
    }

    public GlBuffer getVertexBuffer() {
        return this.vertexBuffer;
    }

    public void upload(ChunkMeshData meshData) {
        this.vertexArray.bind();

        if (this.vertexBuffer != null) {
            this.vertexBuffer.delete();
        }

        VertexData vertexData = meshData.takePendingUpload();

        this.vertexBuffer = new GlMutableBuffer(GL15.GL_STATIC_DRAW);
        this.vertexBuffer.bind(GL15.GL_ARRAY_BUFFER);
        this.vertexBuffer.upload(GL15.GL_ARRAY_BUFFER, vertexData);

        GlVertexFormat<?> vertexFormat = vertexData.format;
        vertexFormat.bindVertexAttributes();
        vertexFormat.enableVertexAttributes();

        int stride = vertexFormat.getStride();

        for (BlockRenderPass pass : BlockRenderPass.VALUES) {
            BufferSlice slice = meshData.getSlice(pass);

            if (slice != null) {
                this.layers[pass.ordinal()] = VertexSlice.pack(slice.start / stride, slice.len / stride);
            }
        }

        this.vertexBuffer.unbind(GL15.GL_ARRAY_BUFFER);
        this.vertexArray.unbind();
    }
}
