package me.jellysquid.mods.sodium.client.render.chunk.vertex.builder;

import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.Material;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ModelQuadEncoder;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ModelQuadFormat;
import org.apache.commons.lang3.Validate;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class ChunkMeshBufferBuilder {
    private final ModelQuadEncoder encoder;
    private final int stride;

    private final int initialCapacity;

    private ByteBuffer buffer;
    private int size;
    private int capacity;
    private int sectionIndex;

    public ChunkMeshBufferBuilder(ModelQuadFormat format, int initialCapacity) {
        this.encoder = format.getEncoder();
        this.stride = format.getStride();

        this.buffer = null;

        this.capacity = initialCapacity;
        this.initialCapacity = initialCapacity;
    }

    public void push(ModelQuadEncoder.Vertex[] vertices, Material material) {
        Validate.isTrue(vertices.length == 4);

        if (this.size + 1 >= this.capacity) {
            this.grow(this.stride);
        }

        this.encoder.write(MemoryUtil.memAddress(this.buffer, this.size * this.stride), material, vertices, this.sectionIndex);
        this.size += 1;
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
        this.size = 0;
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
        return this.size == 0;
    }

    public ByteBuffer slice() {
        if (this.isEmpty()) {
            throw new IllegalStateException("No vertex data in buffer");
        }

        return MemoryUtil.memSlice(this.buffer, 0, this.stride * this.size);
    }

    public int getPrimitiveCount() {
        return this.size;
    }
}
