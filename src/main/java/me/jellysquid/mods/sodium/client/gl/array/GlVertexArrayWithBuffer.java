package me.jellysquid.mods.sodium.client.gl.array;

import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlTessellation;

public class GlVertexArrayWithBuffer implements GlTessellation {
    private final GlBuffer vertexBuffer;
    private final GlVertexArray vertexArray;

    public GlVertexArrayWithBuffer(GlBuffer vertexBuffer, GlVertexArray vertexArray) {
        this.vertexBuffer = vertexBuffer;
        this.vertexArray = vertexArray;
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
    }

    @Override
    public void draw(int mode) {
        this.vertexBuffer.draw(mode);
    }

    public GlBuffer getBuffer() {
        return this.vertexBuffer;
    }
}
