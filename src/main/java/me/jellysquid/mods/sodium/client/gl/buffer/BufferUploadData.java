package me.jellysquid.mods.sodium.client.gl.buffer;

import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;

import java.nio.ByteBuffer;

public class BufferUploadData {
    public final GlVertexFormat<?> format;
    public final ByteBuffer buffer;

    public BufferUploadData(ByteBuffer buffer, GlVertexFormat<?> format) {
        this.format = format;
        this.buffer = buffer;
    }
}
