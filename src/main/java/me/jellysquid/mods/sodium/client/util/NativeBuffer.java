package me.jellysquid.mods.sodium.client.util;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class NativeBuffer {
    private ByteBuffer buffer;

    public NativeBuffer(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public void free() {
        this.check();

        MemoryUtil.memFree(this.buffer);
        this.buffer = null;
    }

    public ByteBuffer getUnsafeBuffer() {
        this.check();

        return this.buffer;
    }

    public int size() {
        this.check();

        return this.buffer.remaining();
    }

    private void check() {
        if (this.buffer == null) {
            throw new IllegalStateException("Buffer has been deleted");
        }
    }
}
