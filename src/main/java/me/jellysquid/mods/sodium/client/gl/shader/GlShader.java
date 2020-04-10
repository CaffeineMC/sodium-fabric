package me.jellysquid.mods.sodium.client.gl.shader;

import me.jellysquid.mods.sodium.client.gl.GlHandle;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL21;

public class GlShader extends GlHandle {
    private static final Logger LOGGER = LogManager.getLogger(GlShader.class);

    private final Identifier name;

    public GlShader(ShaderType type, Identifier name, String src) {
        this.name = name;

        int handle = GL21.glCreateShader(type.id);
        GL21.glShaderSource(handle, src);
        GL21.glCompileShader(handle);

        String log = GL21.glGetShaderInfoLog(handle);

        if (!log.isEmpty()) {
            LOGGER.warn("Shader compilation log for " + this.name + ": " + log);
        }

        int result = GL21.glGetShaderi(handle, GL21.GL_COMPILE_STATUS);

        if (result != GL21.GL_TRUE) {
            throw new RuntimeException("Shader compilation failed, see log for details");
        }

        this.setHandle(handle);
    }

    public Identifier getName() {
        return this.name;
    }

    public void delete() {
        GL21.glDeleteShader(this.handle());

        this.invalidateHandle();
    }
}
