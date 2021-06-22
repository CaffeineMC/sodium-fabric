package me.jellysquid.mods.sodium.client.gl.util;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;

/**
 * Provides a fixed-size queue for batching draw calls for vertex data in the same buffer. This internally
 * uses {@link GL20#glMultiDrawArrays(int, IntBuffer, IntBuffer)} and should be compatible on any relevant platform.
 */
public class GlMultiDrawBatch {
    private final PointerBuffer bufPointer;
    private final IntBuffer bufCount;
    private final IntBuffer bufBaseVertex;

    private int count;

    public GlMultiDrawBatch(int capacity) {
        this.bufPointer = MemoryUtil.memAllocPointer(capacity);
        this.bufCount = MemoryUtil.memAllocInt(capacity);
        this.bufBaseVertex = MemoryUtil.memAllocInt(capacity);
    }

    public PointerBuffer getPointerBuffer() {
        return this.bufPointer;
    }

    public IntBuffer getCountBuffer() {
        return this.bufCount;
    }

    public IntBuffer getBaseVertexBuffer() {
        return this.bufBaseVertex;
    }

    public void begin() {
        this.bufPointer.clear();
        this.bufCount.clear();
        this.bufBaseVertex.clear();

        this.count = 0;
    }

    public void add(int pointer, int count, int baseVertex) {
        int i = this.count++;

        this.bufPointer.put(i, pointer);
        this.bufCount.put(i, count);
        this.bufBaseVertex.put(i, baseVertex);
    }

    public void end() {
        this.bufPointer.limit(this.count);
        this.bufCount.limit(this.count);
        this.bufBaseVertex.limit(this.count);
    }

    public void delete() {
        MemoryUtil.memFree(this.bufPointer);
        MemoryUtil.memFree(this.bufCount);
        MemoryUtil.memFree(this.bufBaseVertex);
    }

    public boolean isEmpty() {
        return this.count <= 0;
    }
}
