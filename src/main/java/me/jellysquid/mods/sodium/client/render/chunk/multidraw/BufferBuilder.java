package me.jellysquid.mods.sodium.client.render.chunk.multidraw;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public abstract class BufferBuilder {
    protected ByteBuffer buffer;
    protected int count;

    private boolean isBuilding;

    protected BufferBuilder(int bytes) {
        this.buffer = MemoryUtil.memAlloc(bytes);
    }

    public int getCount() {
        return this.count;
    }

    public void begin() {
        this.buffer.clear();
        this.count = 0;

        this.isBuilding = true;
    }

    public void end() {
        this.isBuilding = false;
    }

    public boolean isBuilding() {
        return this.isBuilding;
    }

    public ByteBuffer getBuffer() {
        return this.buffer;
    }

    public void delete() {
        MemoryUtil.memFree(this.buffer);
    }
}
