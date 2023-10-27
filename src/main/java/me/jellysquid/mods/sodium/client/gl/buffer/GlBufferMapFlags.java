package me.jellysquid.mods.sodium.client.gl.buffer;

import me.jellysquid.mods.sodium.client.gl.util.EnumBit;
import org.lwjgl.opengl.GL46C;

public enum GlBufferMapFlags implements EnumBit {
    READ(GL46C.GL_MAP_READ_BIT),
    WRITE(GL46C.GL_MAP_WRITE_BIT),
    PERSISTENT(GL46C.GL_MAP_PERSISTENT_BIT),
    INVALIDATE_BUFFER(GL46C.GL_MAP_INVALIDATE_BUFFER_BIT),
    INVALIDATE_RANGE(GL46C.GL_MAP_INVALIDATE_RANGE_BIT),
    EXPLICIT_FLUSH(GL46C.GL_MAP_FLUSH_EXPLICIT_BIT),
    COHERENT(GL46C.GL_MAP_COHERENT_BIT),
    UNSYNCHRONIZED(GL46C.GL_MAP_UNSYNCHRONIZED_BIT);

    private final int bit;

    GlBufferMapFlags(int bit) {
        this.bit = bit;
    }

    @Override
    public int getBits() {
        return this.bit;
    }
}
