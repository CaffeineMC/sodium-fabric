package net.caffeinemc.mods.sodium.client.gl.shader;

import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.opengl.GL40C;

/**
 * An enumeration over the supported OpenGL shader types.
 */
public enum ShaderType {
    VERTEX(GL20C.GL_VERTEX_SHADER),
    GEOMETRY(GL32C.GL_GEOMETRY_SHADER),
    TESS_CONTROL(GL40C.GL_TESS_CONTROL_SHADER),
    TESS_EVALUATION(GL40C.GL_TESS_EVALUATION_SHADER),
    FRAGMENT(GL20C.GL_FRAGMENT_SHADER);

    public final int id;

    ShaderType(int id) {
        this.id = id;
    }

    public static ShaderType fromGlShaderType(int id) {
        for (ShaderType type : values()) {
            if (type.id == id) {
                return type;
            }
        }

        return null;
    }
}
