package me.jellysquid.mods.sodium.render.vertex.type;

import com.mojang.blaze3d.vertex.VertexFormat;

public interface BufferVertexFormat {
    static BufferVertexFormat from(VertexFormat format) {
        return (BufferVertexFormat) format;
    }

    int getStride();
}
