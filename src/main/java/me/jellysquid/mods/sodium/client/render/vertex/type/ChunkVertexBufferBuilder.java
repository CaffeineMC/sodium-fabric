package me.jellysquid.mods.sodium.client.render.vertex.type;

import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import net.minecraft.util.math.Vec3i;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class ChunkVertexBufferBuilder {
    private final ChunkVertexEncoder encoder;
    private final int stride;

    private final int initialCapacity;

    private ByteBuffer buffer;
    private int count;
    private int capacity;

    public ChunkVertexBufferBuilder(ChunkVertexType vertexType, int initialCapacity) {
        this.encoder = vertexType.getEncoder();
        this.stride = vertexType.getVertexFormat().getStride();

        this.buffer = null;

        this.capacity = initialCapacity;
        this.initialCapacity = initialCapacity;
    }

    public void writeVertex(Vec3i offset,
                            float x, float y, float z, int color, float u, float v, int light, int chunk) {
        if (this.count + 1 >= this.capacity) {
            this.grow(this.stride);
        }

        this.encoder.write(MemoryUtil.memAddress(this.buffer, this.count * this.stride),
                offset, x, y, z, color, u, v, light, chunk);
        this.count++;
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

    public int getVertexCount() {
        return this.count;
    }

    public void start() {
        this.count = 0;

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
