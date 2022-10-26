package me.jellysquid.mods.sodium.client.model.vertex.formats.screen_quad;

import me.jellysquid.mods.sodium.client.model.vertex.VertexSink;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.joml.Math;
import org.joml.Matrix4f;


public interface BasicScreenQuadVertexSink extends VertexSink {
    VertexFormat VERTEX_FORMAT = VertexFormats.POSITION_COLOR;

    /**
     * Writes a quad vertex to this sink.
     *
     * @param x The x-position of the vertex
     * @param y The y-position of the vertex
     * @param z The z-position of the vertex
     * @param color The ABGR-packed color of the vertex
     */
    void writeQuad(float x, float y, float z, int color);

    /**
     * Writes a quad vertex to the sink, transformed by the given matrix.
     *
     * @param matrix The matrix to transform the vertex's position by
     */
    default void writeQuad(Matrix4f matrix, float x, float y, float z, int color) {
        float x2 = Math.fma(matrix.m00(), x, Math.fma(matrix.m10(), y, Math.fma(matrix.m20(), z, matrix.m30())));
        float y2 = Math.fma(matrix.m01(), x, Math.fma(matrix.m11(), y, Math.fma(matrix.m21(), z, matrix.m31())));
        float z2 = Math.fma(matrix.m02(), x, Math.fma(matrix.m12(), y, Math.fma(matrix.m22(), z, matrix.m32())));

        this.writeQuad(x2, y2, z2, color);
    }
}
