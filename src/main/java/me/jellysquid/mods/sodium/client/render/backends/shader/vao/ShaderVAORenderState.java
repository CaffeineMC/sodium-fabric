package me.jellysquid.mods.sodium.client.render.backends.shader.vao;

import me.jellysquid.mods.sodium.client.gl.array.GlVertexArray;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeBinding;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.render.backends.ChunkRenderState;
import net.minecraft.util.math.ChunkSectionPos;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

public class ShaderVAORenderState implements ChunkRenderState {
    private final GlBuffer vertexBuffer;
    private final GlVertexArray vertexArray;
    private final ChunkSectionPos origin;

    private boolean init;

    public ShaderVAORenderState(GlBuffer vertexBuffer, ChunkSectionPos origin) {
        this.vertexBuffer = vertexBuffer;
        this.vertexArray = new GlVertexArray();
        this.origin = origin;
    }

    public void unbind() {
        this.vertexArray.unbind();
    }

    @Override
    public void delete() {
        this.vertexBuffer.delete();
        this.vertexArray.delete();
    }

    public void bind(GlVertexAttributeBinding[] attributes) {
        this.vertexArray.bind();

        if (!this.init) {
            this.vertexBuffer.bind(GL15.GL_ARRAY_BUFFER);

            for (GlVertexAttributeBinding binding : attributes) {
                GL20.glVertexAttribPointer(binding.index, binding.count, binding.format, binding.normalized, binding.stride, binding.pointer);
                GL20.glEnableVertexAttribArray(binding.index);
            }

            this.vertexBuffer.unbind(GL15.GL_ARRAY_BUFFER);

            this.init = true;
        }
    }

    public void draw(int mode) {
        this.vertexBuffer.drawArrays(mode);
    }

    public ChunkSectionPos getOrigin() {
        return this.origin;
    }
}
