package me.jellysquid.mods.sodium.client.gl.buffer;

import org.lwjgl.opengl.GL32C;

import static org.lwjgl.opengl.GL43C.GL_SHADER_STORAGE_BUFFER;

public class GlStorageBlock {
    private final int binding;

    public GlStorageBlock(int binding) {
        this.binding = binding;
    }

    public void bindBuffer(GlBuffer buffer) {
        GL32C.glBindBufferBase(GL_SHADER_STORAGE_BUFFER, this.binding, buffer.handle());
    }
}
