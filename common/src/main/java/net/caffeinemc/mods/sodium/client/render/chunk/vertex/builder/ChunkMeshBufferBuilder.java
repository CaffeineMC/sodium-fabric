package net.caffeinemc.mods.sodium.client.render.chunk.vertex.builder;

import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import org.lwjgl.system.MemoryUtil;
import java.nio.ByteBuffer;

public class ChunkMeshBufferBuilder {
    private final ChunkVertexEncoder encoder;
    private final int stride;

    private final int initialCapacity;

    private ByteBuffer buffer;
    private int count;
    private int capacity;
    private int sectionIndex;

    public ChunkMeshBufferBuilder(ChunkVertexType vertexType, int initialCapacity) {
        this.encoder = vertexType.getEncoder();
        this.stride = vertexType.getVertexFormat().getStride();

        this.buffer = null;

        this.capacity = initialCapacity;
        this.initialCapacity = initialCapacity;
    }

    public void push(ChunkVertexEncoder.Vertex[] vertices, Material material) {
        this.push(vertices, material.bits());
    }

    public void push(ChunkVertexEncoder.Vertex[] vertices, int materialBits) {
        var vertexCount = vertices.length;

        if (this.count + vertexCount >= this.capacity) {
            this.grow(this.stride * vertexCount);
        }

        this.encoder.write(MemoryUtil.memAddress(this.buffer, this.count * this.stride),
                materialBits, vertices, this.sectionIndex);

        this.count += vertexCount;
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

    public void start(int sectionIndex) {
        this.count = 0;
        this.sectionIndex = sectionIndex;

        this.setBufferSize(this.initialCapacity);
    }

    public void destroy() {
        if (this.buffer != null) {
            MemoryUtil.memFree(this.buffer);
        }

        this.buffer = null;
    }

    public boolean isEmpty() {
        return this.count == 0;
    }

    public ByteBuffer slice() {
        if (this.isEmpty()) {
            throw new IllegalStateException("No vertex data in buffer");
        }

        return MemoryUtil.memSlice(this.buffer, 0, this.stride * this.count);
    }

    public int count() {
        return this.count;
    }
}
