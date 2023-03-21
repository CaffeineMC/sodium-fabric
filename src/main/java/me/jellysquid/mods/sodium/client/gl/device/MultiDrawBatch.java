package me.jellysquid.mods.sodium.client.gl.device;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Pointer;

import java.nio.BufferOverflowException;
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
        this.pPointer = MemoryUtil.memAddress(MemoryUtil.memCallocPointer(capacity));
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

    public int getMaxVertexCount() {
        int max = 0;

        for (int i = 0; i < this.count; i++) {
            max = Math.max(max, MemoryUtil.memGetInt(this.pCount + (i * 4L)));
        }

        return max;
    }

    int size() {
        return this.count;
    }

    /**
     * Adds a draw command to the buffer.
     *
     * @param elementCount The number of elements in the draw command
     * @param vertexOffset The base vertex offset of the draw command
     * @throws BufferUnderflowException If there is no more space in the command buffer
     */
    public void add(int elementCount, int vertexOffset) {
        if (this.count >= this.capacity) {
            throw new BufferOverflowException();
        }

        MemoryUtil.memPutInt(this.pCount + (this.count * Integer.BYTES), elementCount);
        MemoryUtil.memPutInt(this.pBaseVertex + (this.count * Integer.BYTES), vertexOffset);

        this.count += 1;
    }

    /**
     * Adds a draw command to the buffer conditionally if the {@param condition} value is set to 1. This exists to avoid
     * unnecessary branching, when a draw command is only added if another condition holds true.
     *
     * @param elementCount The number of elements in the draw command
     * @param vertexOffset The base vertex offset of the draw command
     * @param condition A boolean represented as an integer (0 or 1) which conditionally specifies whether the draw
     *                  command will be added to the buffer. If the integer is not one of these values, the behavior
     *                  is undefined.
     * @throws BufferUnderflowException If there is no more space in the command buffer
     */
    public void addConditionally(int elementCount, int vertexOffset, int condition) {
        // Check to make sure we are not about to overflow the buffer, since there are no
        // guard rails here. This will throw even if the condition is false, since we always
        // write a draw command.
        if (this.count >= this.capacity) {
            throw new BufferOverflowException();
        }

        // We always write the draw command even if the condition is false. This is generally safe,
        // since the tail pointer will not be advanced unless the condition is true. It just means
        // that garbage draw commands could exist beyond the tail pointer, which is considered
        // out of bounds, anyway.
        MemoryUtil.memPutInt(this.pCount + (this.count * Integer.BYTES), elementCount);
        MemoryUtil.memPutInt(this.pBaseVertex + (this.count * Integer.BYTES), vertexOffset);

        // The second part of this expression (elementCount != 0 ? ...) is always transformed into
        // a SETcc instruction on x86. This keeps the code branchless.
        this.count += condition & (elementCount != 0 ? 1 : 0);
    }

    public void clear() {
        this.count = 0;
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
