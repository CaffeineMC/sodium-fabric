package me.jellysquid.mods.sodium.client.model.vertex.buffer;

import net.minecraft.client.render.VertexFormat;

import java.nio.ByteBuffer;

public interface VertexBufferView {
    boolean ensureBufferCapacity(int size);

    ByteBuffer getDirectBuffer();

    int getElementOffset();

    void flush(int offset, VertexFormat format);

    VertexFormat getVertexFormat();
}
