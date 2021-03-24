package me.jellysquid.mods.sodium.client.render.chunk.backends.gl20;

import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlMutableBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.VertexData;
import me.jellysquid.mods.sodium.client.gl.util.MemoryTracker;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderContainer;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import me.jellysquid.mods.sodium.client.render.chunk.oneshot.ChunkOneshotGraphicsState;
import org.lwjgl.opengl.GL15;

public class VBOGraphicsState extends ChunkOneshotGraphicsState {
    private final GlBuffer vertexBuffer;
    private GlVertexFormat<?> vertexFormat;

    public VBOGraphicsState(MemoryTracker memoryTracker, ChunkRenderContainer container, int id) {
        super(memoryTracker, container, id);

        this.vertexBuffer = new GlMutableBuffer(GL15.GL_STATIC_DRAW);
    }

    @Override
    public void delete() {
        this.memoryTracker.onMemoryFreeAndRelease(this.vertexBuffer.getSize());

        this.vertexBuffer.delete();
    }

    @Override
    public void upload(ChunkMeshData meshData) {
        VertexData data = meshData.takeVertexData();

        this.vertexBuffer.bind(GL15.GL_ARRAY_BUFFER);

        this.memoryTracker.onMemoryFreeAndRelease(this.vertexBuffer.getSize());
        this.vertexBuffer.upload(GL15.GL_ARRAY_BUFFER, data);
        this.memoryTracker.onMemoryAllocateAndUse(this.vertexBuffer.getSize());

        this.vertexBuffer.unbind(GL15.GL_ARRAY_BUFFER);

        this.vertexFormat = data.format;

        this.setupModelParts(meshData, this.vertexFormat);
    }

    @Override
    public void bind() {
        this.vertexBuffer.bind(GL15.GL_ARRAY_BUFFER);
        this.vertexFormat.bindVertexAttributes();
    }
}
