package me.jellysquid.mods.sodium.client.model.vertex.formats.glyph;

import me.jellysquid.mods.sodium.client.model.vertex.VertexSink;
import me.jellysquid.mods.sodium.client.util.math.Matrix4fExtended;
import me.jellysquid.mods.sodium.client.util.math.MatrixUtil;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.Matrix4f;

public interface GlyphVertexSink extends VertexSink {
    VertexFormat VERTEX_FORMAT = VertexFormats.POSITION_COLOR_TEXTURE_LIGHT;

    default void writeGlyph(Matrix4f matrix, float x, float y, float z, int color, float u, float v, int light) {
        Matrix4fExtended matrixExt = MatrixUtil.getExtendedMatrix(matrix);

        float x2 = matrixExt.transformVecX(x, y, z);
        float y2 = matrixExt.transformVecY(x, y, z);
        float z2 = matrixExt.transformVecZ(x, y, z);

        this.writeGlyph(x2, y2, z2, color, u, v, light);
    }

    void writeGlyph(float x, float y, float z, int color, float u, float v, int light);
}
