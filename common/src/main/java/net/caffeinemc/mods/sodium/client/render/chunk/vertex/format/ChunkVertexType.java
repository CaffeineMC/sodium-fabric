package net.caffeinemc.mods.sodium.client.render.chunk.vertex.format;

import net.caffeinemc.mods.sodium.client.gl.attribute.GlVertexFormat;

public interface ChunkVertexType {
    GlVertexFormat getVertexFormat();

    ChunkVertexEncoder getEncoder();
}
