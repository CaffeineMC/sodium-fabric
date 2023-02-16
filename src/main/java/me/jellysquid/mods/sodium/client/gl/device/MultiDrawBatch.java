package me.jellysquid.mods.sodium.client.gl.device;

import me.jellysquid.mods.sodium.client.gl.util.MemoryUtilHelper;
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

    private int count;

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
        return this.count;
    }

    public void clear() {
        this.count = 0;
    }

    public void add(long pointer, int count, int baseVertex) {
        if (this.count >= this.capacity) {
            throw new BufferUnderflowException();
        }

        MemoryUtil.memPutAddress(this.pPointer + (this.count * Pointer.POINTER_SIZE), pointer);
        MemoryUtil.memPutInt(this.pCount + (this.count * Integer.BYTES), count);
        MemoryUtil.memPutInt(this.pBaseVertex + (this.count * Integer.BYTES), baseVertex);

        this.count++;
    }

    public void delete() {
        MemoryUtil.nmemFree(this.pPointer);
        MemoryUtil.nmemFree(this.pCount);
        MemoryUtil.nmemFree(this.pBaseVertex);
    }

    public boolean isEmpty() {
        return this.count <= 0;
    }

}
