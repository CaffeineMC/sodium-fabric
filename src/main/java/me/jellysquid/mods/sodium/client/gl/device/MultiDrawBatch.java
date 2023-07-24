package me.jellysquid.mods.sodium.client.gl.device;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Pointer;

import java.nio.BufferUnderflowException;
import java.nio.IntBuffer;

/**
 * Provides a fixed-size queue for building a draw-command list usable with
 * {@link org.lwjgl.opengl.GL33C#glMultiDrawElementsBaseVertex(int, IntBuffer, int, PointerBuffer, IntBuffer)}.
 */
public final class MultiDrawBatch {
    private final long pPointer;
    private final long pCount;
    private final long pBaseVertex;

    private final int capacity;

    private int size;

    public MultiDrawBatch(int capacity) {
        this.pPointer = MemoryUtil.memAddress(MemoryUtil.memAllocPointer(capacity));
        this.pCount = MemoryUtil.memAddress(MemoryUtil.memAllocInt(capacity));
        this.pBaseVertex = MemoryUtil.memAddress(MemoryUtil.memAllocInt(capacity));

        this.capacity = capacity;
    }

    long getPointerBuffer() {
        return this.pPointer;
    }

    long getCountBuffer() {
        return this.pCount;
    }

    long getBaseVertexBuffer() {
        return this.pBaseVertex;
    }

    int size() {
        return this.size;
    }

    public void clear() {
        this.size = 0;
    }

    public void add(long pointer, int count, int baseVertex) {
        if (this.size >= this.capacity) {
            throw new BufferUnderflowException();
        }

        long index = this.size++;

        MemoryUtil.memPutAddress(this.pPointer + (index * Pointer.POINTER_SIZE), pointer);
        MemoryUtil.memPutInt(this.pCount + (index * Integer.BYTES), count);
        MemoryUtil.memPutInt(this.pBaseVertex + (index * Integer.BYTES), baseVertex);
    }

    public void delete() {
        MemoryUtil.nmemFree(this.pPointer);
        MemoryUtil.nmemFree(this.pCount);
        MemoryUtil.nmemFree(this.pBaseVertex);
    }

    public boolean isEmpty() {
        return this.size <= 0;
    }

}
