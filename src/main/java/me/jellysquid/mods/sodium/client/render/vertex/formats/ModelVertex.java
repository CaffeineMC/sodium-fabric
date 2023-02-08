package me.jellysquid.mods.sodium.client.render.vertex.formats;

import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.render.RenderGlobal;
import me.jellysquid.mods.sodium.client.render.vertex.buffer.VertexBufferWriter;
import me.jellysquid.mods.sodium.client.render.vertex.VertexFormatDescription;
import me.jellysquid.mods.sodium.client.render.vertex.VertexFormatRegistry;
import me.jellysquid.mods.sodium.common.util.MatrixHelper;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

import static me.jellysquid.mods.sodium.client.render.vertex.VertexElementSerializer.*;

public final class ModelVertex {
    public static final VertexFormatDescription FORMAT = VertexFormatRegistry.get(VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);

    public static final int STRIDE = 36;

    public static void write(long ptr,
                             float x, float y, float z, int color, float u, float v, int overlay, int light, int normal) {
        setPositionXYZ(ptr + 0, x, y, z);
        setColorABGR(ptr + 12, color);
        setTextureUV(ptr + 16, u, v);
        setOverlayUV(ptr + 24, overlay);
        setLightUV(ptr + 28, light);
        setNormalXYZ(ptr + 32, normal);
    }

    public static void writeQuadVertices(VertexBufferWriter writer, MatrixStack.Entry matrices, ModelQuadView quad, int light, int overlay, int color) {
        Matrix3f matNormal = matrices.getNormalMatrix();
        Matrix4f matPosition = matrices.getPositionMatrix();

        try (MemoryStack stack = RenderGlobal.VERTEX_DATA.push()) {
            long buffer = stack.nmalloc(4 * STRIDE);
            long ptr = buffer;

            // The packed transformed normal vector
            var normal = MatrixHelper.transformNormal(matNormal, quad.getNormal());

            for (int i = 0; i < 4; i++) {
                // The position vector
                float x = quad.getX(i);
                float y = quad.getY(i);
                float z = quad.getZ(i);

                // The transformed position vector
                float xt = MatrixHelper.transformPositionX(matPosition, x, y, z);
                float yt = MatrixHelper.transformPositionY(matPosition, x, y, z);
                float zt = MatrixHelper.transformPositionZ(matPosition, x, y, z);

                write(ptr, xt, yt, zt, color, quad.getTexU(i), quad.getTexV(i), overlay, light, normal);
                ptr += STRIDE;
            }

            writer.push(stack, buffer, 4, FORMAT);
        }
    }
}
