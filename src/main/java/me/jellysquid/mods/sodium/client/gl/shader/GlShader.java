package me.jellysquid.mods.sodium.client.gl.shader;

import me.jellysquid.mods.sodium.client.gl.GlObject;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL20C;

/**
 * A compiled OpenGL shader object.
 */
public class GlShader extends GlObject {
    private static final Logger LOGGER = LogManager.getLogger(GlShader.class);

    private final Identifier name;

    public GlShader(ShaderType type, Identifier name, String src) {
        this.name = name;

        int handle = GL20C.glCreateShader(type.id);
        ShaderWorkarounds.safeShaderSource(handle, src);
        GL20C.glCompileShader(handle);

        String log = GL20C.glGetShaderInfoLog(handle);

        if (!log.isEmpty()) {
            LOGGER.warn("Shader compilation log for " + this.name + ": " + log);
        }

        int result = GL20C.glGetShaderi(handle, GL20C.GL_COMPILE_STATUS);

        if (result != GL20C.GL_TRUE) {
            throw new RuntimeException("Shader compilation failed, see log for details");
        }

        this.setHandle(handle);
    }

    public Identifier getName() {
        return this.name;
    }

    public void delete() {
        GL20C.glDeleteShader(this.handle());

        this.invalidateHandle();
    }
}
