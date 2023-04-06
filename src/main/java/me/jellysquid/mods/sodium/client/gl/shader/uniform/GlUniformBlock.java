package me.jellysquid.mods.sodium.client.gl.shader.uniform;

import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import org.lwjgl.opengl.GL32C;

public class GlUniformBlock {
    private final int binding;

    public GlUniformBlock(int uniformBlockBinding) {
        this.binding = uniformBlockBinding;
    }

    public void bindBuffer(GlBuffer buffer) {
        GL32C.glBindBufferBase(GL32C.GL_UNIFORM_BUFFER, this.binding, buffer.handle());
    }

    public void bindBuffer(GlBuffer buffer, int size) {
        //GL32C.glBindBufferBase(GL32C.GL_UNIFORM_BUFFER, this.binding, buffer.handle());
        GL32C.glBindBufferRange(GL32C.GL_UNIFORM_BUFFER, this.binding, buffer.handle(), 0, size);
    }
}
