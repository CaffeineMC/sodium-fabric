package me.jellysquid.mods.sodium.client.render.chunk.data;

import it.unimi.dsi.fastutil.bytes.Byte2ReferenceMap;
import it.unimi.dsi.fastutil.bytes.Byte2ReferenceMaps;
import me.jellysquid.mods.sodium.client.gl.buffer.VertexData;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkModelSlice;

public class ChunkMeshData {
    public static final ChunkMeshData EMPTY = new ChunkMeshData(null, Byte2ReferenceMaps.emptyMap());

    private final Byte2ReferenceMap<ChunkModelSlice> parts;
    private VertexData pendingUpload;

    public ChunkMeshData(VertexData pendingUpload, Byte2ReferenceMap<ChunkModelSlice> parts) {
        this.pendingUpload = pendingUpload;
        this.parts = parts;
    }

    public boolean isEmpty() {
        return this.parts.isEmpty();
    }

    public Byte2ReferenceMap<ChunkModelSlice> getBuffers() {
        return this.parts;
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
        if (this.pendingUpload != null) {
            return this.pendingUpload.buffer.capacity();
        }

        return 0;
    }
}
