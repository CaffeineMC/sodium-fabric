package net.caffeinemc.gfx.opengl.shader.uniform;

import net.caffeinemc.gfx.api.buffer.Buffer;
import net.caffeinemc.gfx.opengl.buffer.GlBuffer;
import net.caffeinemc.gfx.api.shader.UniformBlock;
import org.lwjgl.opengl.GL32C;

// TODO: do not allow this type to bind itself, instead require the pipeline gate to bind ubos
public class GlUniformBlock implements UniformBlock {
    private final int binding;

    public GlUniformBlock(int uniformBlockBinding) {
        this.binding = uniformBlockBinding;
    }

    @Override
    @Deprecated(forRemoval = true)
    public void bindBuffer(Buffer buffer) {
        this.bindBuffer(buffer, 0, buffer.getCapacity());
    }

    @Override
    @Deprecated(forRemoval = true)
    public void bindBuffer(Buffer buffer, int offset, long length) {
        GL32C.glBindBufferRange(GL32C.GL_UNIFORM_BUFFER, this.binding, ((GlBuffer) buffer).handle(), offset, length);
    }
}
