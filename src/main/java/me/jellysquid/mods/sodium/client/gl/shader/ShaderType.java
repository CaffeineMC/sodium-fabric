package me.jellysquid.mods.sodium.client.gl.shader;

import org.lwjgl.opengl.GL46C;

/**
 * An enumeration over the supported OpenGL shader types.
 */
public enum ShaderType {
    VERTEX(GL46C.GL_VERTEX_SHADER),
    FRAGMENT(GL46C.GL_FRAGMENT_SHADER);

    public final int id;

    ShaderType(int id) {
        this.id = id;
    }
}
