package me.jellysquid.mods.sodium.client.render.vertex.formats;

import me.jellysquid.mods.sodium.client.render.vertex.VertexFormatDescription;
import me.jellysquid.mods.sodium.client.render.vertex.VertexFormatRegistry;
import me.jellysquid.mods.sodium.client.util.Norm3b;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
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
                             MatrixStack.Entry matrices,
                             float x, float y, float z, int color, float u, float v, int light, int overlay, int normal) {
        Matrix4f positionMat = matrices.getPositionMatrix();
        Matrix3f normalMat = matrices.getNormalMatrix();

        float x2 = Math.fma(positionMat.m00(), x, Math.fma(positionMat.m10(), y, Math.fma(positionMat.m20(), z, positionMat.m30())));
        float y2 = Math.fma(positionMat.m01(), x, Math.fma(positionMat.m11(), y, Math.fma(positionMat.m21(), z, positionMat.m31())));
        float z2 = Math.fma(positionMat.m02(), x, Math.fma(positionMat.m12(), y, Math.fma(positionMat.m22(), z, positionMat.m32())));

        float normX1 = Norm3b.unpackX(normal);
        float normY1 = Norm3b.unpackY(normal);
        float normZ1 = Norm3b.unpackZ(normal);

        float normX2 = Math.fma(normalMat.m00(), normX1, Math.fma(normalMat.m10(), normY1, normalMat.m20() * normZ1));
        float normY2 = Math.fma(normalMat.m01(), normX1, Math.fma(normalMat.m11(), normY1, normalMat.m21() * normZ1));
        float normZ2 = Math.fma(normalMat.m02(), normX1, Math.fma(normalMat.m12(), normY1, normalMat.m22() * normZ1));

        ModelVertex.write(ptr, x2, y2, z2, color, u, v, light, overlay, Norm3b.pack(normX2, normY2, normZ2));
    }

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
}
