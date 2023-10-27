package me.jellysquid.mods.sodium.client.gl.shader.uniform;

import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import org.lwjgl.opengl.GL46C;

public class GlUniformBlock {
    private final int binding;

    public GlUniformBlock(int uniformBlockBinding) {
        this.binding = uniformBlockBinding;
    }

    public void bindBuffer(GlBuffer buffer) {
        GL46C.glBindBufferBase(GL46C.GL_UNIFORM_BUFFER, this.binding, buffer.handle());
    }
}
