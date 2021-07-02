package me.jellysquid.mods.sodium.client.util;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class NativeBuffer {
    private ByteBuffer buffer;

    public NativeBuffer(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public void free() {
        ByteBuffer buffer = this.getUnsafeBuffer();
        MemoryUtil.memFree(buffer);

        this.buffer = null;
    }

    public ByteBuffer getUnsafeBuffer() {
        if (this.buffer == null) {
            throw new IllegalStateException("Buffer has been deleted");
        }

        return this.buffer;
    }

    public int size() {
        return this.getUnsafeBuffer()
                .remaining();
    }

    @Override
    protected void finalize() {
        if (this.buffer != null) {
            MemoryUtil.memFree(this.buffer);

            this.buffer = null;
        }
    }
}
