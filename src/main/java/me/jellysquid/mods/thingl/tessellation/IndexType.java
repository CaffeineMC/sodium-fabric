package me.jellysquid.mods.thingl.tessellation;

import org.lwjgl.opengl.GL32C;

public enum IndexType {
    UNSIGNED_BYTE(GL32C.GL_UNSIGNED_BYTE, 1),
    UNSIGNED_SHORT(GL32C.GL_UNSIGNED_SHORT, 2),
    UNSIGNED_INT(GL32C.GL_UNSIGNED_INT, 4);

    private final int id;
    private final int stride;

    IndexType(int id, int stride) {
        this.id = id;
        this.stride = stride;
    }

    public int getFormatId() {
        return this.id;
    }

    public int getStride() {
        return this.stride;
    }

    public static final IndexType[] VALUES = IndexType.values();
}
