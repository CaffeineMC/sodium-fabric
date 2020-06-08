package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.gl.buffer.VertexData;
import me.jellysquid.mods.sodium.client.gl.util.BufferSlice;
import me.jellysquid.mods.sodium.client.render.layer.BlockRenderPass;

import java.util.Collections;
import java.util.Map;

public class ChunkMeshData {
    public static final ChunkMeshData EMPTY = new ChunkMeshData(null, Collections.emptyMap());

    private final Map<BlockRenderPass, BufferSlice> layers;
    private VertexData pendingUpload;

    public ChunkMeshData(VertexData pendingUpload, Map<BlockRenderPass, BufferSlice> layers) {
        this.pendingUpload = pendingUpload;
        this.layers = layers;
    }

    public boolean isEmpty() {
        return this.layers.isEmpty();
    }

    public Iterable<? extends BlockRenderPass> getLayers() {
        return this.layers.keySet();
    }

    public BufferSlice getSlice(BlockRenderPass pass) {
        return this.layers.get(pass);
    }

    public VertexData takePendingUpload() {
        VertexData data = this.pendingUpload;

        if (data == null) {
            throw new NullPointerException("No pending data to upload");
        }

        this.pendingUpload = null;

        return data;
    }

    public boolean hasData() {
        return this.pendingUpload != null;
    }

    public int getSize() {
        // TODO: this is ugly...
        if (this.pendingUpload == null) {
            return 0;
        }

        return this.pendingUpload.buffer.capacity();
    }
}
