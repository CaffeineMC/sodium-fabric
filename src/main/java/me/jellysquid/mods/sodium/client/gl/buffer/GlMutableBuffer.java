package me.jellysquid.mods.sodium.client.gl.buffer;

/**
 * A mutable buffer type which is supported with OpenGL 1.5+. The buffer's storage can be reallocated at any time
 * without needing to re-create the buffer itself.
 */
public class GlMutableBuffer extends GlBuffer {
    private long size = 0L;

    public GlMutableBuffer() {
        super();
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getSize() {
        return this.size;
    }
}
