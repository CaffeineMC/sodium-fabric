package me.jellysquid.mods.sodium.client.gl.tessellation;

import me.jellysquid.mods.sodium.client.gl.array.GlVertexArray;
import me.jellysquid.mods.sodium.client.gl.attribute.GlAttributeBinding;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import org.lwjgl.opengl.GL20;

public class GlVertexArrayWithBuffer implements GlTessellation {
    private final GlBuffer vertexBuffer;
    private final GlVertexArray vertexArray;
    private final GlAttributeBinding[] attributes;

    private boolean init;

    public GlVertexArrayWithBuffer(GlBuffer vertexBuffer, GlAttributeBinding[] attributes) {
        this.vertexBuffer = vertexBuffer;
        this.vertexArray = new GlVertexArray();
        this.attributes = attributes;
    }

    @Override
    public void unbind() {
        this.vertexArray.unbind();
    }

    @Override
    public void delete() {
        this.vertexBuffer.delete();
        this.vertexArray.delete();
    }

    @Override
    public void bind() {
        this.vertexArray.bind();

        if (!this.init) {
            this.vertexBuffer.bind();

            for (GlAttributeBinding binding : this.attributes) {
                GL20.glVertexAttribPointer(binding.index, binding.count, binding.format, binding.normalized, binding.stride, binding.pointer);
                GL20.glEnableVertexAttribArray(binding.index);
            }

            this.vertexBuffer.unbind();

            this.init = true;
        }
    }

    @Override
    public void draw(int mode) {
        this.vertexBuffer.drawArrays(mode);
    }
}
