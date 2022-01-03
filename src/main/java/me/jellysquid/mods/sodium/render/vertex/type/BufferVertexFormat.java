package me.jellysquid.mods.sodium.render.vertex.type;

import net.minecraft.client.render.VertexFormat;

public interface BufferVertexFormat {
    static BufferVertexFormat from(VertexFormat format) {
        return (BufferVertexFormat) format;
    }

    int getStride();
}
