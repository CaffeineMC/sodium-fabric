package me.jellysquid.mods.thingl.tessellation;

import org.lwjgl.opengl.GL20C;

public enum PrimitiveType {
    TRIANGLES(GL20C.GL_TRIANGLES);

    private final int id;

    PrimitiveType(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }
}
