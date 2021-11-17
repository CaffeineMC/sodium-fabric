package me.jellysquid.mods.sodium.client.gl.shader;

import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL43C;

/**
 * An enumeration over the supported OpenGL shader types.
 */
public enum ShaderType {
    VERTEX(GL20C.GL_VERTEX_SHADER),
    FRAGMENT(GL20C.GL_FRAGMENT_SHADER),
    COMPUTE(GL43C.GL_COMPUTE_SHADER);

    public final int id;

    ShaderType(int id) {
        this.id = id;
    }
}
