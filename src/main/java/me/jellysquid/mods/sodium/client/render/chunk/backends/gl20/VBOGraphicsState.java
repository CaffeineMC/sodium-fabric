package me.jellysquid.mods.sodium.client.render.chunk.backends.gl20;

import it.unimi.dsi.fastutil.bytes.Byte2ReferenceMap;
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

public class VBOGraphicsState implements ChunkGraphicsState {
    private final GlBuffer buffer;
    private final ChunkModelPart[] parts;
    private final boolean[] presentLayers;

    public VBOGraphicsState() {
        this.buffer = new GlMutableBuffer(GL15.GL_STATIC_DRAW);
        this.parts = new ChunkModelPart[ChunkModelPart.count()];
        this.presentLayers = new boolean[BlockRenderPass.COUNT];
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

        for (Byte2ReferenceMap.Entry<ChunkModelSlice> entry : meshData.getBuffers().byte2ReferenceEntrySet()) {
            this.parts[entry.getByteKey()] = new ChunkModelPart(entry.getValue().start / stride, entry.getValue().len / stride);
            this.presentLayers[entry.getValue().pass.ordinal()] = true;
        }
    }

    public ChunkModelPart getModelPart(byte key) {
        return this.parts[key];
    }

    public GlBuffer getBuffer() {
        return this.buffer;
    }

    public boolean containsDataForPass(BlockRenderPass pass) {
        return this.presentLayers[pass.ordinal()];
    }
}
