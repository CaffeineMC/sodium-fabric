package me.jellysquid.mods.sodium.opengl.attribute;

import net.minecraft.client.render.VertexFormat;

public interface BufferVertexFormat {
    static BufferVertexFormat from(VertexFormat format) {
        return (BufferVertexFormat) format;
    }

    int getStride();
}
