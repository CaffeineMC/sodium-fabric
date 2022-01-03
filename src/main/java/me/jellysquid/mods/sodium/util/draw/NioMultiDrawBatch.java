package me.jellysquid.mods.sodium.util.draw;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;

class NioMultiDrawBatch implements MultiDrawBatch {
    private final PointerBuffer bufPointer;
    private final IntBuffer bufCount;
    private final IntBuffer bufBaseVertex;

    private int count;

    NioMultiDrawBatch(int capacity) {
        this.bufPointer = MemoryUtil.memAllocPointer(capacity);
        this.bufCount = MemoryUtil.memAllocInt(capacity);
        this.bufBaseVertex = MemoryUtil.memAllocInt(capacity);
    }

    @Override
    public PointerBuffer getPointerBuffer() {
        return this.bufPointer;
    }

    @Override
    public IntBuffer getCountBuffer() {
        return this.bufCount;
    }

    @Override
    public IntBuffer getBaseVertexBuffer() {
        return this.bufBaseVertex;
    }

    @Override
    public void begin() {
        this.bufPointer.clear();
        this.bufCount.clear();
        this.bufBaseVertex.clear();

        this.count = 0;
    }

    @Override
    public void add(long pointer, int count, int baseVertex) {
        int i = this.count++;

        this.bufPointer.put(i, pointer);
        this.bufCount.put(i, count);
        this.bufBaseVertex.put(i, baseVertex);
    }

    @Override
    public void end() {
        this.bufPointer.limit(this.count);
        this.bufCount.limit(this.count);
        this.bufBaseVertex.limit(this.count);
    }

    @Override
    public void delete() {
        MemoryUtil.memFree(this.bufPointer);
        MemoryUtil.memFree(this.bufCount);
        MemoryUtil.memFree(this.bufBaseVertex);
    }

    @Override
    public boolean isEmpty() {
        return this.count <= 0;
    }
}
