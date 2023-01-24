package me.jellysquid.mods.sodium.client.render.vertex.formats;

import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.render.vertex.VertexBufferWriter;
import me.jellysquid.mods.sodium.client.render.vertex.VertexFormatDescription;
import me.jellysquid.mods.sodium.client.render.vertex.VertexFormatRegistry;
import me.jellysquid.mods.sodium.client.util.Norm3b;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public final class ModelVertex {
    public static final VertexFormatDescription FORMAT = VertexFormatRegistry.get(VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);

    public static final int STRIDE = 36;

    private static final int OFFSET_POSITION = 0;
    private static final int OFFSET_COLOR = 12;
    private static final int OFFSET_TEXTURE = 16;
    private static final int OFFSET_OVERLAY = 24;
    private static final int OFFSET_LIGHT = 28;
    private static final int OFFSET_NORMAL = 32;

    public static void write(long ptr,
                             float x, float y, float z, int color, float u, float v, int light, int overlay, int normal) {
        MemoryUtil.memPutFloat(ptr + OFFSET_POSITION + 0, x);
        MemoryUtil.memPutFloat(ptr + OFFSET_POSITION + 4, y);
        MemoryUtil.memPutFloat(ptr + OFFSET_POSITION + 8, z);

        MemoryUtil.memPutInt(ptr + OFFSET_COLOR, color);

        MemoryUtil.memPutFloat(ptr + OFFSET_TEXTURE + 0, u);
        MemoryUtil.memPutFloat(ptr + OFFSET_TEXTURE + 4, v);

        MemoryUtil.memPutInt(ptr + OFFSET_LIGHT, light);

        MemoryUtil.memPutInt(ptr + OFFSET_OVERLAY, overlay);

        MemoryUtil.memPutInt(ptr + OFFSET_NORMAL, normal);
    }

    public static void writeQuadVertices(VertexBufferWriter writer, MatrixStack.Entry matrices, ModelQuadView quad, int light, int overlay, int color) {
        Matrix3f matNormal = matrices.getNormalMatrix();
        Matrix4f matPosition = matrices.getPositionMatrix();

        try (MemoryStack stack = VertexBufferWriter.STACK.push()) {
            long buffer = writer.buffer(stack, 4, STRIDE, FORMAT);
            long ptr = buffer;

            // The packed normal vector
            var n = quad.getNormal();

            // The normal vector
            float nx = Norm3b.unpackX(n);
            float ny = Norm3b.unpackY(n);
            float nz = Norm3b.unpackZ(n);

            // The transformed normal vector
            float nxt = (matNormal.m00() * nx) + (matNormal.m10() * ny) + (matNormal.m20() * nz);
            float nyt = (matNormal.m01() * nx) + (matNormal.m11() * ny) + (matNormal.m21() * nz);
            float nzt = (matNormal.m02() * nx) + (matNormal.m12() * ny) + (matNormal.m22() * nz);

            // The packed transformed normal vector
            var nt = Norm3b.pack(nxt, nyt, nzt);

            for (int i = 0; i < 4; i++) {
                // The position vector
                float x = quad.getX(i);
                float y = quad.getY(i);
                float z = quad.getZ(i);

                // The transformed position vector
                float xt = (matPosition.m00() * x) + (matPosition.m10() * y) + (matPosition.m20() * z) + matPosition.m30();
                float yt = (matPosition.m01() * x) + (matPosition.m11() * y) + (matPosition.m21() * z) + matPosition.m31();
                float zt = (matPosition.m02() * x) + (matPosition.m12() * y) + (matPosition.m22() * z) + matPosition.m32();

                write(ptr, xt, yt, zt, color, quad.getTexU(i), quad.getTexV(i), light, overlay, nt);
                ptr += STRIDE;
            }

            writer.push(buffer, 4, STRIDE, FORMAT);
        }
    }
}
