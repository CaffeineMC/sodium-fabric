package me.jellysquid.mods.sodium.client.gl.buffer;

import me.jellysquid.mods.sodium.client.gl.util.EnumBit;
import org.lwjgl.opengl.GL46C;

public enum GlBufferStorageFlags implements EnumBit {
    PERSISTENT(GL46C.GL_MAP_PERSISTENT_BIT),
    MAP_READ(GL46C.GL_MAP_READ_BIT),
    MAP_WRITE(GL46C.GL_MAP_WRITE_BIT),
    CLIENT_STORAGE(GL46C.GL_CLIENT_STORAGE_BIT),
    COHERENT(GL46C.GL_MAP_COHERENT_BIT);

    private final int bits;

    GlBufferStorageFlags(int bits) {
        this.bits = bits;
    }

    @Override
    public int getBits() {
        return this.bits;
    }
}
