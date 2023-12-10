package me.jellysquid.mods.sodium.client.render.chunk.vertex.format;

import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;

public interface ModelQuadFormat {
    ModelQuadEncoder getEncoder();

    int getStride();
}
