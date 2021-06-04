package me.jellysquid.mods.sodium.client.gl.tessellation;

import org.lwjgl.opengl.GL32C;

public enum GlPrimitiveType {
    LINES(GL32C.GL_LINES),
    TRIANGLES(GL32C.GL_TRIANGLES),
    QUADS(GL32C.GL_TRIANGLES);

    private final int id;

    GlPrimitiveType(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }
}
