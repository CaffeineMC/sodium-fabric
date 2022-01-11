package me.jellysquid.mods.sodium.opengl.shader.uniform;

import me.jellysquid.mods.sodium.opengl.buffer.Buffer;
import me.jellysquid.mods.sodium.opengl.buffer.BufferImpl;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.opengl.GL45C;

public class UniformBlock {
    private final int binding;

    public UniformBlock(int uniformBlockBinding) {
        this.binding = uniformBlockBinding;
    }

    public void bindBuffer(Buffer buffer) {
        GL32C.glBindBufferBase(GL32C.GL_UNIFORM_BUFFER, this.binding, ((BufferImpl) buffer).handle());
    }

    public void bindBuffer(Buffer buffer, int offset, int length) {
        GL32C.glBindBufferRange(GL32C.GL_UNIFORM_BUFFER, this.binding, ((BufferImpl) buffer).handle(), offset, length);
    }
}
