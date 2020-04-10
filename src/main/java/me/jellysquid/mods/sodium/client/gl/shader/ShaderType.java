package me.jellysquid.mods.sodium.client.gl.shader;

import org.lwjgl.opengl.GL21;

public enum ShaderType {
    VERTEX(GL21.GL_VERTEX_SHADER),
    FRAGMENT(GL21.GL_FRAGMENT_SHADER);

    public final int id;

    ShaderType(int id) {
        this.id = id;
    }
}
