package me.jellysquid.mods.sodium.client.model.consumer;

import me.jellysquid.mods.sodium.client.util.math.Matrix4fExtended;
import me.jellysquid.mods.sodium.client.util.math.MatrixUtil;
import net.minecraft.client.util.math.MatrixStack;

public interface QuadVertexConsumer {
    /**
     * Writes a vertex directly into the consumer with no additional processing. This requires callers to do some
     * upfront work to encode their values.
     *  @param x       The x-position of the vertex
     * @param y       The y-position of the vertex
     * @param z       The z-position of the vertex
     * @param color   The color of the vertex in little-endian RGBA format
     * @param u       The u-position of the texture
     * @param v       The v-position of the texture
     * @param light   The light of the vertex
     * @param overlay The overlay (shadow) of the vertex
     * @param normal  The normal of the vertex
     */
    void vertexQuad(float x, float y, float z, int color, float u, float v, int light, int overlay, int normal);

    default void vertexQuad(MatrixStack.Entry entry, float x, float y, float z, int color, float u, float v, int light, int overlay, int normal) {
        Matrix4fExtended modelMatrix = MatrixUtil.getExtendedMatrix(entry.getModel());

        float x2 = modelMatrix.transformVecX(x, y, z);
        float y2 = modelMatrix.transformVecY(x, y, z);
        float z2 = modelMatrix.transformVecZ(x, y, z);

        int norm = MatrixUtil.transformPackedNormal(normal, entry.getNormal());

        this.vertexQuad(x2, y2, z2, color, u, v, light, overlay, norm);
    }
}
