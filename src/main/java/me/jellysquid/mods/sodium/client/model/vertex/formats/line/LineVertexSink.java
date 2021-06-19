package me.jellysquid.mods.sodium.client.model.vertex.formats.line;

import me.jellysquid.mods.sodium.client.model.vertex.VertexSink;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;

public interface LineVertexSink extends VertexSink {
    VertexFormat VERTEX_FORMAT = VertexFormats.LINES;

    /**
     * Writes a line vertex to the sink.
     * @param x The x-position of the vertex
     * @param y The y-position of the vertex
     * @param z The z-position of the vertex
     * @param color The ABGR-packed color of the vertex
     * @param normal The 3 byte packed normal vector of the vertex
     */
    void vertexLine(float x, float y, float z, int color, int normal);
}
