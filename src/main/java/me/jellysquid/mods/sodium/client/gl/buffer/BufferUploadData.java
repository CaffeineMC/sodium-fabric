package me.jellysquid.mods.sodium.client.gl.buffer;

import net.minecraft.client.render.VertexFormat;

import java.nio.ByteBuffer;

public class BufferUploadData {
    public final VertexFormat format;
    public final ByteBuffer buffer;

    public BufferUploadData(ByteBuffer buffer, VertexFormat format) {
        this.format = format;
        this.buffer = buffer;
    }
}
