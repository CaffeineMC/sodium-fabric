package me.jellysquid.mods.sodium.mixin.features.buffer_builder.intrinsics;

import me.jellysquid.mods.sodium.client.render.RenderGlobal;
import me.jellysquid.mods.sodium.client.render.vertex.formats.LineVertex;
import me.jellysquid.mods.sodium.client.render.vertex.VertexBufferWriter;
import me.jellysquid.mods.sodium.client.util.Norm3b;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;

import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(WorldRenderer.class)
public class MixinWorldRenderer {
    /**
     * @author JellySquid
     * @reason Use intrinsics where possible to speed up vertex writing
     */
    @Overwrite
    public static void drawBox(MatrixStack matrices, VertexConsumer vertexConsumer, double x1, double y1, double z1,
                               double x2, double y2, double z2, float red, float green, float blue, float alpha,
                               float xAxisRed, float yAxisGreen, float zAxisBlue) {
        Matrix4f position = matrices.peek().getPositionMatrix();
        Matrix3f normal = matrices.peek().getNormalMatrix();

        float x1f = (float) x1;
        float y1f = (float) y1;
        float z1f = (float) z1;
        float x2f = (float) x2;
        float y2f = (float) y2;
        float z2f = (float) z2;

        int color = ColorABGR.pack(red, green, blue, alpha);

        float v1x = Math.fma(position.m00(), x1f, Math.fma(position.m10(), y1f, Math.fma(position.m20(), z1f, position.m30())));
        float v1y = Math.fma(position.m01(), x1f, Math.fma(position.m11(), y1f, Math.fma(position.m21(), z1f, position.m31())));
        float v1z = Math.fma(position.m02(), x1f, Math.fma(position.m12(), y1f, Math.fma(position.m22(), z1f, position.m32())));

        float v2x = Math.fma(position.m00(), x2f, Math.fma(position.m10(), y1f, Math.fma(position.m20(), z1f, position.m30())));
        float v2y = Math.fma(position.m01(), x2f, Math.fma(position.m11(), y1f, Math.fma(position.m21(), z1f, position.m31())));
        float v2z = Math.fma(position.m02(), x2f, Math.fma(position.m12(), y1f, Math.fma(position.m22(), z1f, position.m32())));

        float v3x = Math.fma(position.m00(), x1f, Math.fma(position.m10(), y2f, Math.fma(position.m20(), z1f, position.m30())));
        float v3y = Math.fma(position.m01(), x1f, Math.fma(position.m11(), y2f, Math.fma(position.m21(), z1f, position.m31())));
        float v3z = Math.fma(position.m02(), x1f, Math.fma(position.m12(), y2f, Math.fma(position.m22(), z1f, position.m32())));

        float v4x = Math.fma(position.m00(), x1f, Math.fma(position.m10(), y1f, Math.fma(position.m20(), z2f, position.m30())));
        float v4y = Math.fma(position.m01(), x1f, Math.fma(position.m11(), y1f, Math.fma(position.m21(), z2f, position.m31())));
        float v4z = Math.fma(position.m02(), x1f, Math.fma(position.m12(), y1f, Math.fma(position.m22(), z2f, position.m32())));

        float v5x = Math.fma(position.m00(), x2f, Math.fma(position.m10(), y2f, Math.fma(position.m20(), z1f, position.m30())));
        float v5y = Math.fma(position.m01(), x2f, Math.fma(position.m11(), y2f, Math.fma(position.m21(), z1f, position.m31())));
        float v5z = Math.fma(position.m02(), x2f, Math.fma(position.m12(), y2f, Math.fma(position.m22(), z1f, position.m32())));

        float v6x = Math.fma(position.m00(), x1f, Math.fma(position.m10(), y2f, Math.fma(position.m20(), z2f, position.m30())));
        float v6y = Math.fma(position.m01(), x1f, Math.fma(position.m11(), y2f, Math.fma(position.m21(), z2f, position.m31())));
        float v6z = Math.fma(position.m02(), x1f, Math.fma(position.m12(), y2f, Math.fma(position.m22(), z2f, position.m32())));

        float v7x = Math.fma(position.m00(), x2f, Math.fma(position.m10(), y1f, Math.fma(position.m20(), z2f, position.m30())));
        float v7y = Math.fma(position.m01(), x2f, Math.fma(position.m11(), y1f, Math.fma(position.m21(), z2f, position.m31())));
        float v7z = Math.fma(position.m02(), x2f, Math.fma(position.m12(), y1f, Math.fma(position.m22(), z2f, position.m32())));

        float v8x = Math.fma(position.m00(), x2f, Math.fma(position.m10(), y2f, Math.fma(position.m20(), z2f, position.m30())));
        float v8y = Math.fma(position.m01(), x2f, Math.fma(position.m11(), y2f, Math.fma(position.m21(), z2f, position.m31())));
        float v8z = Math.fma(position.m02(), x2f, Math.fma(position.m12(), y2f, Math.fma(position.m22(), z2f, position.m32())));

        var writer = VertexBufferWriter.of(vertexConsumer);

        writeLineVertices(writer, v1x, v1y, v1z, ColorABGR.pack(red, yAxisGreen, zAxisBlue, alpha), Norm3b.pack(normal.m00(), normal.m10(), normal.m20()));
        writeLineVertices(writer, v2x, v2y, v2z, ColorABGR.pack(red, yAxisGreen, zAxisBlue, alpha), Norm3b.pack(normal.m00(), normal.m10(), normal.m20()));
        writeLineVertices(writer, v1x, v1y, v1z, ColorABGR.pack(xAxisRed, green, zAxisBlue, alpha), Norm3b.pack(normal.m01(), normal.m11(), normal.m21()));
        writeLineVertices(writer, v3x, v3y, v3z, ColorABGR.pack(xAxisRed, green, zAxisBlue, alpha), Norm3b.pack(normal.m01(), normal.m11(), normal.m21()));
        writeLineVertices(writer, v1x, v1y, v1z, ColorABGR.pack(xAxisRed, yAxisGreen, blue, alpha), Norm3b.pack(normal.m02(), normal.m12(), normal.m22()));
        writeLineVertices(writer, v4x, v4y, v4z, ColorABGR.pack(xAxisRed, yAxisGreen, blue, alpha), Norm3b.pack(normal.m02(), normal.m12(), normal.m22()));
        writeLineVertices(writer, v2x, v2y, v2z, color, Norm3b.pack(normal.m01(), normal.m11(), normal.m21()));
        writeLineVertices(writer, v5x, v5y, v5z, color, Norm3b.pack(normal.m01(), normal.m11(), normal.m21()));
        writeLineVertices(writer, v5x, v5y, v5z, color, Norm3b.pack(-normal.m00(), -normal.m10(), -normal.m20()));
        writeLineVertices(writer, v3x, v3y, v3z, color, Norm3b.pack(-normal.m00(), -normal.m10(), -normal.m20()));
        writeLineVertices(writer, v3x, v3y, v3z, color, Norm3b.pack(normal.m02(), normal.m12(), normal.m22()));
        writeLineVertices(writer, v6x, v6y, v6z, color, Norm3b.pack(normal.m02(), normal.m12(), normal.m22()));
        writeLineVertices(writer, v6x, v6y, v6z, color, Norm3b.pack(-normal.m01(), -normal.m11(), -normal.m21()));
        writeLineVertices(writer, v4x, v4y, v4z, color, Norm3b.pack(-normal.m01(), -normal.m11(), -normal.m21()));
        writeLineVertices(writer, v4x, v4y, v4z, color, Norm3b.pack(normal.m00(), normal.m10(), normal.m20()));
        writeLineVertices(writer, v7x, v7y, v7z, color, Norm3b.pack(normal.m00(), normal.m10(), normal.m20()));
        writeLineVertices(writer, v7x, v7y, v7z, color, Norm3b.pack(-normal.m02(), -normal.m12(), -normal.m22()));
        writeLineVertices(writer, v2x, v2y, v2z, color, Norm3b.pack(-normal.m02(), -normal.m12(), -normal.m22()));
        writeLineVertices(writer, v6x, v6y, v6z, color, Norm3b.pack(normal.m00(), normal.m10(), normal.m20()));
        writeLineVertices(writer, v8x, v8y, v8z, color, Norm3b.pack(normal.m00(), normal.m10(), normal.m20()));
        writeLineVertices(writer, v7x, v7y, v7z, color, Norm3b.pack(normal.m01(), normal.m11(), normal.m21()));
        writeLineVertices(writer, v8x, v8y, v8z, color, Norm3b.pack(normal.m01(), normal.m11(), normal.m21()));
        writeLineVertices(writer, v5x, v5y, v5z, color, Norm3b.pack(normal.m02(), normal.m12(), normal.m22()));
        writeLineVertices(writer, v8x, v8y, v8z, color, Norm3b.pack(normal.m02(), normal.m12(), normal.m22()));
    }

    private static void writeLineVertices(VertexBufferWriter writer, float x, float y, float z, int color, int normal) {
        try (MemoryStack stack = RenderGlobal.VERTEX_DATA.push()) {
            long buffer = stack.nmalloc(2 * LineVertex.STRIDE);
            long ptr = buffer;

            for (int i = 0; i < 2; i++) {
                LineVertex.write(ptr, x, y, z, color, normal);
                ptr += LineVertex.STRIDE;
            }

            writer.push(stack, buffer, 2, LineVertex.FORMAT);
        }

    }

}
