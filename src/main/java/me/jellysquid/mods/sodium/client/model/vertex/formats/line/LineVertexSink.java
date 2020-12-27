package me.jellysquid.mods.sodium.client.model.vertex.formats.line;

import me.jellysquid.mods.sodium.client.model.vertex.VertexSink;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;

public interface LineVertexSink extends VertexSink {
    VertexFormat VERTEX_FORMAT = VertexFormats.POSITION_COLOR;

    /**
     * Writes a line vertex to the sink.
     *
     * @param x The x-position of the vertex
     * @param y The y-position of the vertex
     * @param z The z-position of the vertex
     * @param color The ABGR-packed color of the vertex
     */
    void vertexLine(float x, float y, float z, int color);

    /**
     * Writes a line vertex to the sink using unpacked normalized colors. This is slower than
     * {@link LineVertexSink#vertexLine(float, float, float, int)} as it needs to pack the colors
     * each call.
     *
     * @param x The x-position of the vertex
     * @param y The y-position of the vertex
     * @param z The z-position of the vertex
     * @param r The normalized red component of the vertex's color
     * @param g The normalized green component of the vertex's color
     * @param b The normalized blue component of the vertex's color
     * @param a The normalized alpha component of the vertex's color
     */
    default void vertexLine(float x, float y, float z, float r, float g, float b, float a) {
        this.vertexLine(x, y, z, ColorABGR.pack(r, g, b, a));
    }
}
