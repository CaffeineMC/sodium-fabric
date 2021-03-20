package me.jellysquid.mods.sodium.client.render.chunk.multidraw;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public abstract class StructBuffer {
    protected ByteBuffer buffer;

    protected StructBuffer(int bytes, int stride) {
        this.buffer = MemoryUtil.memAlloc(bytes * stride);
    }

    public ByteBuffer getBuffer() {
        return this.buffer;
    }

    public void delete() {
        MemoryUtil.memFree(this.buffer);
    }

    public long getBufferAddress() {
        return MemoryUtil.memAddress(this.buffer);
    }
}
