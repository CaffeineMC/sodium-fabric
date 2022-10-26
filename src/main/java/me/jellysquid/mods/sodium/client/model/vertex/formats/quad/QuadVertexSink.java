package me.jellysquid.mods.sodium.client.model.vertex.formats.quad;

import me.jellysquid.mods.sodium.client.model.vertex.VertexSink;
import me.jellysquid.mods.sodium.client.util.Norm3b;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public interface QuadVertexSink extends VertexSink {
    VertexFormat VERTEX_FORMAT = VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL;

    /**
     * Writes a quad vertex to this sink.
     *
     * @param x The x-position of the vertex
     * @param y The y-position of the vertex
     * @param z The z-position of the vertex
     * @param color The ABGR-packed color of the vertex
     * @param u The u-texture of the vertex
     * @param v The y-texture of the vertex
     * @param light The packed light-map coordinates of the vertex
     * @param overlay The packed overlay-map coordinates of the vertex
     * @param normal The 3-byte packed normal vector of the vertex
     */
    void writeQuad(float x, float y, float z, int color, float u, float v, int light, int overlay, int normal);

    /**
     * Writes a quad vertex to the sink, transformed by the given matrices.
     *
     * @param matrices The matrices to transform the vertex's position and normal vectors by
     */
    default void writeQuad(MatrixStack.Entry matrices, float x, float y, float z, int color, float u, float v, int light, int overlay, int normal) {
        Matrix4f matrix = matrices.getPositionMatrix();
        Matrix3f normMatrix = matrices.getNormalMatrix();

        float x2 = Math.fma(matrix.m00(), x, Math.fma(matrix.m10(), y, Math.fma(matrix.m20(), z, matrix.m30())));
        float y2 = Math.fma(matrix.m01(), x, Math.fma(matrix.m11(), y, Math.fma(matrix.m21(), z, matrix.m31())));
        float z2 = Math.fma(matrix.m02(), x, Math.fma(matrix.m12(), y, Math.fma(matrix.m22(), z, matrix.m32())));

        float normX1 = Norm3b.unpackX(normal);
        float normY1 = Norm3b.unpackY(normal);
        float normZ1 = Norm3b.unpackZ(normal);

        float normX2 = Math.fma(normMatrix.m00(), normX1, Math.fma(normMatrix.m10(), normY1, normMatrix.m20() * normZ1));
        float normY2 = Math.fma(normMatrix.m01(), normX1, Math.fma(normMatrix.m11(), normY1, normMatrix.m21() * normZ1));
        float normZ2 = Math.fma(normMatrix.m02(), normX1, Math.fma(normMatrix.m12(), normY1, normMatrix.m22() * normZ1));

        this.writeQuad(x2, y2, z2, color, u, v, light, overlay, Norm3b.pack(normX2, normY2, normZ2));
    }
}
