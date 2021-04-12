package me.jellysquid.mods.sodium.client.gl.tessellation;

import org.lwjgl.opengl.GL11;

public enum GlPrimitiveType {
    LINES(GL11.GL_LINES),
    TRIANGLES(GL11.GL_TRIANGLES),
    QUADS(GL11.GL_QUADS);

    private final int id;

    GlPrimitiveType(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }
}
