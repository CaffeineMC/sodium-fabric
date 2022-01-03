package me.jellysquid.mods.sodium.util.draw;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Pointer;

import java.nio.BufferUnderflowException;
import java.nio.IntBuffer;

class DirectMultiDrawBatch implements MultiDrawBatch {
    private final PointerBuffer bufPointer;
    private final IntBuffer bufCount;
    private final IntBuffer bufBaseVertex;

    private long bufPointerAddr;
    private long bufCountAddr;
    private long bufBaseVertexAddr;

    private final int capacity;

    private int count;

    DirectMultiDrawBatch(int capacity) {
        this.bufPointer = MemoryUtil.memAllocPointer(capacity);
        this.bufCount = MemoryUtil.memAllocInt(capacity);
        this.bufBaseVertex = MemoryUtil.memAllocInt(capacity);
        this.capacity = capacity;

        this.resetPointers();
    }

    private void resetPointers() {
        this.bufPointerAddr = MemoryUtil.memAddress(this.bufPointer);
        this.bufCountAddr = MemoryUtil.memAddress(this.bufCount);
        this.bufBaseVertexAddr = MemoryUtil.memAddress(this.bufBaseVertex);
    }

    @Override
    public PointerBuffer getPointerBuffer() {
        return MemoryUtil.memPointerBuffer(MemoryUtil.memAddress(this.bufPointer), this.count);
    }

    @Override
    public IntBuffer getCountBuffer() {
        return MemoryUtil.memIntBuffer(MemoryUtil.memAddress(this.bufCount), this.count);
    }

    @Override
    public IntBuffer getBaseVertexBuffer() {
        return MemoryUtil.memIntBuffer(MemoryUtil.memAddress(this.bufBaseVertex), this.count);
    }

    @Override
    public void begin() {
        this.count = 0;

        this.resetPointers();
    }

    @Override
    public void add(long pointer, int count, int baseVertex) {
        if (this.count >= this.capacity) {
            throw new BufferUnderflowException();
        }

        MemoryUtil.memPutLong(this.bufPointerAddr, pointer);
        this.bufPointerAddr += Pointer.POINTER_SIZE;

        MemoryUtil.memPutInt(this.bufCountAddr, count);
        this.bufCountAddr += 4;

        MemoryUtil.memPutInt(this.bufBaseVertexAddr, baseVertex);
        this.bufBaseVertexAddr += 4;

        this.count++;
    }

    @Override
    public void end() {

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
