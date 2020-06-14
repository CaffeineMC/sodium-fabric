package me.jellysquid.mods.sodium.client.render.chunk.backends.gl30;

import it.unimi.dsi.fastutil.bytes.Byte2ReferenceMap;
import me.jellysquid.mods.sodium.client.gl.array.GlVertexArray;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlMutableBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.VertexData;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkModelPart;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkModelSlice;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import org.lwjgl.opengl.GL15;

public class VAOGraphicsState implements ChunkGraphicsState {
    private final GlVertexArray vertexArray;
    private GlBuffer vertexBuffer;

    private final ChunkModelPart[] parts;
    private final boolean[] presentLayers;

    public VAOGraphicsState() {
        this.vertexBuffer = new GlMutableBuffer(GL15.GL_STATIC_DRAW);
        this.vertexArray = new GlVertexArray();
        this.parts = new ChunkModelPart[ChunkModelPart.count()];
        this.presentLayers = new boolean[BlockRenderPass.COUNT];
    }

    @Override
    public void delete() {
        this.vertexBuffer.delete();
        this.vertexArray.delete();
    }

    public ChunkModelPart getModelPart(byte key) {
        return this.parts[key];
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

        for (Byte2ReferenceMap.Entry<ChunkModelSlice> entry : meshData.getBuffers().byte2ReferenceEntrySet()) {
            this.parts[entry.getByteKey()] = new ChunkModelPart(entry.getValue().start / stride, entry.getValue().len / stride);
            this.presentLayers[entry.getValue().pass.ordinal()] = true;
        }

        this.vertexBuffer.unbind(GL15.GL_ARRAY_BUFFER);
        this.vertexArray.unbind();
    }

    public boolean containsDataForPass(BlockRenderPass pass) {
        return this.presentLayers[pass.ordinal()];
    }
}
