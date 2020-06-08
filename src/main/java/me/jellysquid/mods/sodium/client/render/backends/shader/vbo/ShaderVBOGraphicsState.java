package me.jellysquid.mods.sodium.client.render.backends.shader.vbo;

import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlMutableBuffer;
import me.jellysquid.mods.sodium.client.gl.util.BufferSlice;
import me.jellysquid.mods.sodium.client.gl.util.VertexSlice;
import me.jellysquid.mods.sodium.client.render.backends.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkMeshData;
import me.jellysquid.mods.sodium.client.render.layer.BlockRenderPass;
import net.minecraft.util.math.ChunkSectionPos;
import org.lwjgl.opengl.GL15;

public class ShaderVBOGraphicsState implements ChunkGraphicsState {
    private final GlVertexFormat<?> vertexFormat;
    private final GlBuffer buffer;
    private final ChunkSectionPos translation;
    private final long[] layers;

    public ShaderVBOGraphicsState(GlVertexFormat<?> vertexFormat, ChunkSectionPos origin) {
        this.vertexFormat = vertexFormat;
        this.translation = origin;
        this.buffer = new GlMutableBuffer(GL15.GL_STATIC_DRAW);
        this.layers = new long[BlockRenderPass.count()];
    }

    @Override
    public void delete() {
        this.buffer.delete();
    }

    public ChunkSectionPos getOrigin() {
        return this.translation;
    }

    public void upload(ChunkMeshData meshData) {
        this.buffer.bind(GL15.GL_ARRAY_BUFFER);
        this.buffer.upload(GL15.GL_ARRAY_BUFFER, meshData.takePendingUpload());
        this.buffer.unbind(GL15.GL_ARRAY_BUFFER);

        int stride = this.vertexFormat.getStride();

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
