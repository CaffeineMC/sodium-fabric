package me.jellysquid.mods.sodium.client.render.chunk.format;

import me.jellysquid.mods.sodium.client.model.vertex.VertexSink;

public interface ModelVertexSink extends VertexSink {
    /**
     * Writes a quad vertex to this sink.
     * @param x The x-position of the vertex
     * @param y The y-position of the vertex
     * @param z The z-position of the vertex
     * @param color The ABGR-packed color of the vertex
     * @param u The u-texture of the vertex
     * @param v The y-texture of the vertex
     * @param light The packed light-map coordinates of the vertex
     */
    void writeQuad(float x, float y, float z, int color, float u, float v, int light);
}
