package me.jellysquid.mods.sodium.client.gl.buffer;

import me.jellysquid.mods.sodium.client.gl.util.EnumBit;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL44C;

public enum GlBufferMapFlags implements EnumBit {
    READ(GL30C.GL_MAP_READ_BIT),
    WRITE(GL30C.GL_MAP_WRITE_BIT),
    PERSISTENT(GL44C.GL_MAP_PERSISTENT_BIT),
    INVALIDATE_BUFFER(GL30C.GL_MAP_INVALIDATE_BUFFER_BIT),
    INVALIDATE_RANGE(GL30C.GL_MAP_INVALIDATE_RANGE_BIT),
    EXPLICIT_FLUSH(GL30C.GL_MAP_FLUSH_EXPLICIT_BIT),
    COHERENT(GL44C.GL_MAP_COHERENT_BIT);

    private final int bit;

    GlBufferMapFlags(int bit) {
        this.bit = bit;
    }

    @Override
    public int getBits() {
        return this.bit;
    }
}
