package me.jellysquid.mods.sodium.client.render.chunk.backends.gl30;

import me.jellysquid.mods.sodium.client.gl.array.GlVertexArray;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlMutableBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.VertexData;
import me.jellysquid.mods.sodium.client.gl.util.MemoryTracker;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderContainer;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import me.jellysquid.mods.sodium.client.render.chunk.oneshot.ChunkOneshotGraphicsState;
import org.lwjgl.opengl.GL15;

public class VAOGraphicsState extends ChunkOneshotGraphicsState {
    private final GlVertexArray vertexArray;
    private final GlBuffer vertexBuffer;

    public VAOGraphicsState(MemoryTracker memoryTracker, ChunkRenderContainer container, int id) {
        super(memoryTracker, container, id);

        this.vertexBuffer = new GlMutableBuffer(GL15.GL_STATIC_DRAW);
        this.vertexArray = new GlVertexArray();
    }

    @Override
    public void upload(ChunkMeshData meshData) {
        VertexData vertexData = meshData.takeVertexData();

        this.vertexArray.bind();

        this.vertexBuffer.bind(GL15.GL_ARRAY_BUFFER);

        this.memoryTracker.onMemoryFreeAndRelease(this.vertexBuffer.getSize());
        this.vertexBuffer.upload(GL15.GL_ARRAY_BUFFER, vertexData);
        this.memoryTracker.onMemoryAllocateAndUse(this.vertexBuffer.getSize());

        GlVertexFormat<?> vertexFormat = vertexData.format;
        vertexFormat.bindVertexAttributes();
        vertexFormat.enableVertexAttributes();

        this.setupModelParts(meshData, vertexFormat);

        this.vertexBuffer.unbind(GL15.GL_ARRAY_BUFFER);
        this.vertexArray.unbind();
    }

    @Override
    public void bind() {
        this.vertexArray.bind();
    }

    @Override
    public void delete() {
        this.memoryTracker.onMemoryFreeAndRelease(this.vertexBuffer.getSize());

        this.vertexBuffer.delete();
        this.vertexArray.delete();
    }
}
