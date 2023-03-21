package me.jellysquid.mods.sodium.client.util.memory;

import org.lwjgl.system.MemoryUtil;

import java.nio.BufferOverflowException;

public class HeapArena {
    private final long buffer;

    private final int capacity;
    private int position;

    public HeapArena(int capacity) {
        this.capacity = capacity;
        this.buffer = MemoryUtil.nmemCalloc(1, capacity);
    }

    public long alloc(int alignment, int bytes) {
        int offset = this.position;
        offset = align(offset, alignment);

        int tail = offset + bytes;

        if (tail > this.capacity) {
            throw new BufferOverflowException();
        }

        this.position = tail;

        return this.buffer + offset;
    }

    private static int align(int offset, int alignment) {
        return (offset + (alignment - 1)) & -alignment;
    }

    public void reset() {
        MemoryUtil.memSet(this.buffer, 0, this.position);

        this.position = 0;
    }

    public void free() {
        MemoryUtil.nmemFree(this.buffer);
    }
}
