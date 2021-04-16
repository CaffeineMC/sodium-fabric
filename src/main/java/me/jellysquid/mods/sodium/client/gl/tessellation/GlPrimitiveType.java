package me.jellysquid.mods.sodium.client.gl.tessellation;

import org.lwjgl.opengl.GL20C;

public enum GlPrimitiveType {
    LINES(GL20C.GL_LINES),
    TRIANGLES(GL20C.GL_TRIANGLES),
    QUADS(GL20C.GL_QUADS);

    private final int id;

    GlPrimitiveType(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }
}
