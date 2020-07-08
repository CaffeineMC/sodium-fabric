package me.jellysquid.mods.sodium.client.gl.buffer;

import org.lwjgl.opengl.GL15;

import java.nio.ByteBuffer;

/**
 * A mutable buffer type which is supported with OpenGL 1.5+. The buffer's storage can be reallocated at any time
 * without needing to re-create the buffer itself.
 */
public class GlMutableBuffer extends GlBuffer {
    private final int hints;

    public GlMutableBuffer(int hints) {
        this.hints = hints;
    }

    @Override
    public void upload(int target, ByteBuffer buf) {
        GL15.glBufferData(target, buf, this.hints);
        this.size = buf.capacity();
    }

    @Override
    public void allocate(int target, int size) {
        GL15.glBufferData(target, size, this.hints);
        this.size = size;
    }

    public void invalidate(int target) {
        this.allocate(target, 0);
    }
}
