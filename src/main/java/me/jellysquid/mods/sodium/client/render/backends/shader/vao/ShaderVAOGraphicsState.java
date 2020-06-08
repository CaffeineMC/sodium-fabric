package me.jellysquid.mods.sodium.client.render.backends.shader.vao;

import me.jellysquid.mods.sodium.client.gl.array.GlVertexArray;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlMutableBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.VertexData;
import me.jellysquid.mods.sodium.client.gl.util.BufferSlice;
import me.jellysquid.mods.sodium.client.gl.util.VertexSlice;
import me.jellysquid.mods.sodium.client.render.backends.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkMeshData;
import me.jellysquid.mods.sodium.client.render.layer.BlockRenderPass;
import net.minecraft.util.math.ChunkSectionPos;
import org.lwjgl.opengl.GL15;

public class ShaderVAOGraphicsState implements ChunkGraphicsState {
    private final GlVertexFormat<?> vertexFormat;

    private final GlVertexArray vertexArray;
    private GlBuffer vertexBuffer;

    private final ChunkSectionPos origin;
    private final long[] layers;

    public ShaderVAOGraphicsState(GlVertexFormat<?> vertexFormat, ChunkSectionPos origin) {
        this.vertexFormat = vertexFormat;
        this.vertexBuffer = new GlMutableBuffer(GL15.GL_STATIC_DRAW);
        this.vertexArray = new GlVertexArray();
        this.layers = new long[BlockRenderPass.count()];

        this.origin = origin;
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

    public ChunkSectionPos getOrigin() {
        return this.origin;
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

        this.vertexFormat.bindVertexAttributes();
        this.vertexFormat.enableVertexAttributes();

        int stride = this.vertexFormat.getStride();

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
