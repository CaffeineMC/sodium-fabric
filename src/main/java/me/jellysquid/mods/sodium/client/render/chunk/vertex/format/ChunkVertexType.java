package me.jellysquid.mods.sodium.client.render.chunk.vertex.format;

import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;

public interface ChunkVertexType {
    GlVertexFormat<?> getVertexFormat();

    ChunkVertexEncoder getEncoder();

    default String getDefine() {
        return "VERTEX_FORMAT_COMPACT"; // as in original Sodium all vertex formats would use this mode
    }
}
