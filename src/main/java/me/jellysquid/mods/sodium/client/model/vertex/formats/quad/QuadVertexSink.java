package me.jellysquid.mods.sodium.client.model.vertex.formats.quad;

import me.jellysquid.mods.sodium.client.model.vertex.VertexSink;
import me.jellysquid.mods.sodium.client.util.math.Matrix4fExtended;
import me.jellysquid.mods.sodium.client.util.math.MatrixUtil;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;

public interface QuadVertexSink extends VertexSink {
    VertexFormat VERTEX_FORMAT = VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL;

    void writeQuad(float x, float y, float z, int color, float u, float v, int light, int overlay, int normal);

    default void writeQuad(MatrixStack.Entry entry, float x, float y, float z, int color, float u, float v, int light, int overlay, int normal) {
        Matrix4fExtended modelMatrix = MatrixUtil.getExtendedMatrix(entry.getModel());

        float x2 = modelMatrix.transformVecX(x, y, z);
        float y2 = modelMatrix.transformVecY(x, y, z);
        float z2 = modelMatrix.transformVecZ(x, y, z);

        int norm = MatrixUtil.transformPackedNormal(normal, entry.getNormal());

        this.writeQuad(x2, y2, z2, color, u, v, light, overlay, norm);
    }
}
