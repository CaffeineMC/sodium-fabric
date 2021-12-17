package me.jellysquid.mods.sodium.client.gl.shader;

import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GL43C;

public class GlShaderStorageBlock {
    private final int index;

    public GlShaderStorageBlock(int index) {
        this.index = index;
    }

    public void bindBuffer(GlBuffer buffer) {
        GL43C.glBindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, this.index, buffer.handle());
    }
}
