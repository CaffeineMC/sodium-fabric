package me.jellysquid.mods.thingl.shader;

import org.lwjgl.opengl.GL20C;

/**
 * An enumeration over the supported OpenGL shader types.
 */
public enum ShaderType {
    VERTEX(GL20C.GL_VERTEX_SHADER, ".v.glsl"),
    FRAGMENT(GL20C.GL_FRAGMENT_SHADER, ".f.glsl");

    private final int id;
    private final String extension;

    ShaderType(int id, String extension) {
        this.id = id;
        this.extension = extension;
    }

    public int getId() {
        return this.id;
    }

    public String getExtension() {
        return this.extension;
    }
}
