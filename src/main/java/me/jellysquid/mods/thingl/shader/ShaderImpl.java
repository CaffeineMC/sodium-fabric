package me.jellysquid.mods.thingl.shader;

import com.mojang.blaze3d.platform.GlStateManager;
import me.jellysquid.mods.thingl.GlObject;
import me.jellysquid.mods.thingl.device.RenderDeviceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL20C;

/**
 * A compiled OpenGL shader object.
 */
public class ShaderImpl extends GlObject implements Shader {
    private static final Logger LOGGER = LogManager.getLogger(ShaderImpl.class);

    public ShaderImpl(RenderDeviceImpl device, ShaderType type, String src) {
        super(device);

        int handle = GL20C.glCreateShader(type.getId());
        ShaderWorkarounds.safeShaderSource(handle, src);
        GL20C.glCompileShader(handle);

        String log = GL20C.glGetShaderInfoLog(handle);

        if (!log.isEmpty()) {
            LOGGER.warn("Shader compilation log: " + log);
        }

        int result = GlStateManager.glGetShaderi(handle, GL20C.GL_COMPILE_STATUS);

        if (result != GL20C.GL_TRUE) {
            throw new RuntimeException("Shader compilation failed, see log for details");
        }

        this.setHandle(handle);
    }

    public void delete() {
        GL20C.glDeleteShader(this.handle());

        this.invalidateHandle();
    }
}
