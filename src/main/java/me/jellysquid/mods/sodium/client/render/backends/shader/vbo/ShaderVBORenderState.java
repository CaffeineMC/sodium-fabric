package me.jellysquid.mods.sodium.client.render.backends.shader.vbo;

import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeBinding;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.render.backends.ChunkRenderState;
import net.minecraft.util.math.ChunkSectionPos;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

public class ShaderVBORenderState implements ChunkRenderState {
    private final GlBuffer buffer;
    private final ChunkSectionPos translation;

    public ShaderVBORenderState(GlBuffer buffer, ChunkSectionPos origin) {
        this.translation = origin;
        this.buffer = buffer;
    }

    public void bind(GlVertexAttributeBinding[] attributes) {
        this.buffer.bind(GL15.GL_ARRAY_BUFFER);

        for (GlVertexAttributeBinding binding : attributes) {
            GL20.glVertexAttribPointer(binding.index, binding.count, binding.format, binding.normalized, binding.stride, binding.pointer);
        }
    }

    public void draw(int mode) {
        this.buffer.drawArrays(mode);
    }

    public void unbind() {
        this.buffer.unbind(GL15.GL_ARRAY_BUFFER);
    }

    @Override
    public void delete() {
        this.buffer.delete();
    }

    public ChunkSectionPos getOrigin() {
        return this.translation;
    }
}
