package me.jellysquid.mods.sodium.client.gl.buffer;

import me.jellysquid.mods.sodium.client.gl.util.EnumBit;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL44C;

public enum GlBufferStorageFlags implements EnumBit {
    PERSISTENT(GL44C.GL_MAP_PERSISTENT_BIT),
    MAP_READ(GL30C.GL_MAP_READ_BIT),
    MAP_WRITE(GL30C.GL_MAP_WRITE_BIT),
    CLIENT_STORAGE(GL44C.GL_CLIENT_STORAGE_BIT),
    COHERENT(GL44C.GL_MAP_COHERENT_BIT);

    private final int bits;

    GlBufferStorageFlags(int bits) {
        this.bits = bits;
    }

    @Override
    public int getBits() {
        return this.bits;
    }
}
