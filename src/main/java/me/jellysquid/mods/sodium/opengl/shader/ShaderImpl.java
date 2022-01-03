package me.jellysquid.mods.sodium.opengl.shader;

import com.mojang.blaze3d.platform.GlStateManager;
import me.jellysquid.mods.sodium.opengl.GlObject;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL20C;

/**
 * A compiled OpenGL shader object.
 */
public class ShaderImpl extends GlObject {
    private static final Logger LOGGER = LogManager.getLogger(ShaderImpl.class);

    private final Identifier name;

    public ShaderImpl(ShaderType type, Identifier name, String src) {
        this.name = name;

        int handle = GL20C.glCreateShader(type.id);
        ShaderWorkarounds.safeShaderSource(handle, src);
        GL20C.glCompileShader(handle);

        String log = GL20C.glGetShaderInfoLog(handle);

        if (!log.isEmpty()) {
            LOGGER.warn("Shader compilation log for " + this.name + ": " + log);
        }

        int result = GlStateManager.glGetShaderi(handle, GL20C.GL_COMPILE_STATUS);

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
