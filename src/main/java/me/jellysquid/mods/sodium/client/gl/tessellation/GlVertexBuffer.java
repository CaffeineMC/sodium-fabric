package me.jellysquid.mods.sodium.client.gl.tessellation;

import me.jellysquid.mods.sodium.client.gl.attribute.GlAttributeBinding;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import org.lwjgl.opengl.GL20;

public class GlVertexBuffer implements GlTessellation {
    private final GlBuffer buffer;
    private final GlAttributeBinding[] attributes;

    public GlVertexBuffer(GlBuffer buffer, GlAttributeBinding[] attributes) {
        this.buffer = buffer;
        this.attributes = attributes;
    }

    @Override
    public void bind() {
        this.buffer.bind();

        for (GlAttributeBinding binding : this.attributes) {
            GL20.glVertexAttribPointer(binding.index, binding.count, binding.format, binding.normalized, binding.stride, binding.pointer);
        }
    }

    @Override
    public void draw(int mode) {
        this.buffer.drawArrays(mode);
    }

    @Override
    public void unbind() {
        this.buffer.unbind();
    }

    @Override
    public void delete() {
        this.buffer.delete();
    }
}
