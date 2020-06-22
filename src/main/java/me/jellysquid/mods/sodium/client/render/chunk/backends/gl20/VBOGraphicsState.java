package me.jellysquid.mods.sodium.client.render.chunk.backends.gl20;

import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlMutableBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.VertexData;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import me.jellysquid.mods.sodium.client.render.chunk.oneshot.ChunkOneshotGraphicsState;
import org.lwjgl.opengl.GL15;

public class VBOGraphicsState extends ChunkOneshotGraphicsState {
    private final GlBuffer buffer;
    private GlVertexFormat<?> vertexFormat;

    public VBOGraphicsState() {
        this.buffer = new GlMutableBuffer(GL15.GL_STATIC_DRAW);
    }

    @Override
    public void delete() {
        this.buffer.delete();
    }

    @Override
    public void upload(ChunkMeshData meshData) {
        VertexData data = meshData.takeVertexData();

        this.buffer.bind(GL15.GL_ARRAY_BUFFER);
        this.buffer.upload(GL15.GL_ARRAY_BUFFER, data);
        this.buffer.unbind(GL15.GL_ARRAY_BUFFER);

        this.vertexFormat = data.format;

        this.setupModelParts(meshData, this.vertexFormat);
    }

    @Override
    public void bind() {
        this.buffer.bind(GL15.GL_ARRAY_BUFFER);
        this.vertexFormat.bindVertexAttributes();
    }

    public GlBuffer getBuffer() {
        return this.buffer;
    }
}
