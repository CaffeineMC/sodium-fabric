package net.caffeinemc.mods.sodium.api.buffer;

import net.caffeinemc.mods.sodium.api.memory.MemoryIntrinsics;
import net.minecraft.client.util.GlAllocationUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

/**
 * Provides a growing buffer with a push method for convenience.
 * Otherwise, this class is very un-managed.
 */
public class UnmanagedBufferBuilder {
    private ByteBuffer buffer;
    private int byteOffset = 0;

    public UnmanagedBufferBuilder(int initialCapacity) {
        this.buffer = GlAllocationUtils.allocateByteBuffer(initialCapacity);
    }

    public void ensureCapacity(int capacity) {
        if (capacity > this.buffer.capacity()) {
            ByteBuffer byteBuffer = GlAllocationUtils.resizeByteBuffer(this.buffer, capacity);
            byteBuffer.rewind();
            this.buffer = byteBuffer;
        }
    }

    /**
     * Copies memory from the stack onto the end of this buffer builder.
     */
    public void push(MemoryStack ignoredStack, long src, int size) {
        ensureCapacity(byteOffset + size);

        long dst = MemoryUtil.memAddress(this.buffer, this.byteOffset);
        MemoryIntrinsics.copyMemory(src, dst, size);
        byteOffset += size;
    }

    public Built build() {
        return new Built(this.byteOffset, MemoryUtil.memSlice(this.buffer, 0, this.byteOffset));
    }

    public void reset() {
        this.byteOffset = 0;
    }

    /**
     * Builds and resets this builder.
     * Make sure to use/upload the return value before pushing more data.
     * @return a ByteBuffer containing all the data pushed to this builder
     */
    public Built end() {
        int endOffset = this.byteOffset;
        this.byteOffset = 0;
        return new Built(endOffset, MemoryUtil.memSlice(this.buffer, 0, endOffset));
    }

    public static class Built {
        public int size;
        public ByteBuffer buffer;

        Built(int size, ByteBuffer buffer) {
            this.size = size;
            this.buffer = buffer;
        }
    }
}
