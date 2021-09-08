package me.jellysquid.mods.thingl.shader.uniform;

import me.jellysquid.mods.thingl.buffer.Buffer;
import me.jellysquid.mods.thingl.buffer.BufferImpl;
import org.lwjgl.opengl.GL32C;

public class GlUniformBlock {
    private final int binding;

    public GlUniformBlock(int uniformBlockBinding) {
        this.binding = uniformBlockBinding;
    }

    public void bindBuffer(Buffer buffer) {
        GL32C.glBindBufferBase(GL32C.GL_UNIFORM_BUFFER, this.binding, buffer.getGlId());
    }
}
