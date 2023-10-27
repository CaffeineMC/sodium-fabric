package me.jellysquid.mods.sodium.client.gl.tessellation;

import org.lwjgl.opengl.GL46C;

public enum GlPrimitiveType {
    TRIANGLES(GL46C.GL_TRIANGLES);

    private final int id;

    GlPrimitiveType(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }
}
