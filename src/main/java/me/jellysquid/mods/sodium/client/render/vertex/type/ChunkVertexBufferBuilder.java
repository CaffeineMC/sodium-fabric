package me.jellysquid.mods.sodium.client.render.vertex.type;

import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class ChunkVertexBufferBuilder {
    private final ChunkVertexEncoder encoder;
    private final int stride;

    private final int initialCapacity;

    private ByteBuffer buffer;
    private int count;
    private int capacity;
    private int chunkId;

    public ChunkVertexBufferBuilder(ChunkVertexType vertexType, int initialCapacity) {
        this.encoder = vertexType.getEncoder();
        this.stride = vertexType.getVertexFormat().getStride();

        this.buffer = null;

        this.capacity = initialCapacity;
        this.initialCapacity = initialCapacity;
    }

    public int push(ChunkVertexEncoder.Vertex[] vertices) {
        var vertexStart = this.count;
        var vertexCount = vertices.length;

        if (this.count + vertexCount >= this.capacity) {
            this.grow(this.stride * vertexCount);
        }

        long ptr = MemoryUtil.memAddress(this.buffer, this.count * this.stride);

        for (ChunkVertexEncoder.Vertex vertex : vertices) {
            ptr = this.encoder.write(ptr, vertex, this.chunkId);
        }

        this.count += vertexCount;

        return vertexStart;
    }

    private void grow(int len) {
        // The new capacity will at least as large as the write it needs to service
        int cap = Math.max(this.capacity * 2, this.capacity + len);

        // Update the buffer and capacity now
        this.setBufferSize(cap * this.stride);
    }

    private void setBufferSize(int capacity) {
        this.buffer = MemoryUtil.memRealloc(this.buffer, capacity * this.stride);
        this.capacity = capacity;
    }

    public void start(int chunkId) {
        this.count = 0;
        this.chunkId = chunkId;

        this.setBufferSize(this.initialCapacity);
    }

    public void destroy() {
        if (this.buffer != null) {
            MemoryUtil.memFree(this.buffer);
        }

        this.buffer = null;
    }

    public NativeBuffer pop() {
        if (this.count == 0) {
            return null;
        }

        return NativeBuffer.copy(MemoryUtil.memSlice(this.buffer, 0, this.stride * this.count));
    }
}
