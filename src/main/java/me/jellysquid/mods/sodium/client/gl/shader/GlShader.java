package me.jellysquid.mods.sodium.client.gl.shader;

import me.jellysquid.mods.sodium.client.gl.GlHandle;
import org.lwjgl.opengl.GL21;

public class GlShader extends GlHandle {
    public GlShader(ShaderType type, String src) {
        int handle = GL21.glCreateShader(type.id);
        GL21.glShaderSource(handle, src);
        GL21.glCompileShader(handle);

        int result = GL21.glGetShaderi(handle, GL21.GL_COMPILE_STATUS);

        if (result != GL21.GL_TRUE) {
            throw new RuntimeException("Shader compilation error: " + GL21.glGetShaderInfoLog(handle));
        }

        this.setHandle(handle);
    }

    public void delete() {
        GL21.glDeleteShader(this.handle());

        this.invalidateHandle();
    }
}
