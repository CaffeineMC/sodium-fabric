package net.caffeinemc.gfx.api.draw;

import org.lwjgl.system.MemoryUtil;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

public class DrawCommandEncoder {
    private static final int STRIDE = 20;

    private final ByteBuffer buffer;

    private final int capacity;
    private int size;

    public DrawCommandEncoder(ByteBuffer buffer) {
        this.buffer = buffer;
        this.capacity = buffer.remaining() / STRIDE;
        this.size = 0;
    }

    public void push(int count, int instanceCount, int firstIndex, int baseVertex, int baseInstance) {
        if (this.size >= this.capacity) {
            throw new BufferOverflowException();
        }

        long ptr = MemoryUtil.memAddress(this.buffer, this.size++ * STRIDE);
        MemoryUtil.memPutInt(ptr + 0, count);
        MemoryUtil.memPutInt(ptr + 4, instanceCount);
        MemoryUtil.memPutInt(ptr + 8, firstIndex);
        MemoryUtil.memPutInt(ptr + 12, baseVertex);
        MemoryUtil.memPutInt(ptr + 16, baseInstance);
    }

    public void push(int count, int firstIndex) {
        this.push(count, 1, firstIndex, 0, 0);
    }

    public ByteBuffer slice() {
        return MemoryUtil.memSlice(this.buffer, 0, this.size * STRIDE);
    }

    public int size() {
        return this.size;
    }
}
