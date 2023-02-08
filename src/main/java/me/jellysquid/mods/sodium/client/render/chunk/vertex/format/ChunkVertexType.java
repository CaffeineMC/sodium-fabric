package me.jellysquid.mods.sodium.client.render.chunk.vertex.format;

import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;

public interface ChunkVertexType {
    /**
     * @return The scale to be applied to vertex coordinates
     */
    float getPositionScale();

    /**
     * @return The translation to be applied to vertex coordinates
     */
    float getPositionOffset();

    /**
     * @return The scale to be applied to texture coordinates
     */
    float getTextureScale();

    GlVertexFormat<ChunkMeshAttribute> getVertexFormat();

    ChunkVertexEncoder getEncoder();
}
