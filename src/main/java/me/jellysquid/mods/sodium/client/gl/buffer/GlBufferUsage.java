package me.jellysquid.mods.sodium.client.gl.buffer;

import org.lwjgl.opengl.GL15;

public enum GlBufferUsage {
    GL_STREAM_DRAW(GL15.GL_STREAM_DRAW),
    GL_STREAM_READ(GL15.GL_STREAM_READ),
    GL_STREAM_COPY(GL15.GL_STREAM_COPY),
    GL_STATIC_DRAW(GL15.GL_STATIC_DRAW),
    GL_STATIC_READ(GL15.GL_STATIC_READ),
    GL_STATIC_COPY(GL15.GL_STATIC_COPY),
    GL_DYNAMIC_DRAW(GL15.GL_DYNAMIC_DRAW),
    GL_DYNAMIC_READ(GL15.GL_DYNAMIC_READ),
    GL_DYNAMIC_COPY(GL15.GL_DYNAMIC_COPY);

    private final int id;

    GlBufferUsage(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }
}
