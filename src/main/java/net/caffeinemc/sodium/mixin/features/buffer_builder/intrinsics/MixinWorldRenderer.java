package net.caffeinemc.sodium.mixin.features.buffer_builder.intrinsics;

import net.caffeinemc.sodium.interop.vanilla.math.matrix.Matrix4fUtil;
import net.caffeinemc.sodium.interop.vanilla.vertex.VanillaVertexFormats;
import net.caffeinemc.sodium.render.vertex.VertexDrain;
import net.caffeinemc.sodium.interop.vanilla.vertex.formats.LineVertexSink;
import net.caffeinemc.sodium.util.packed.Normal3b;
import net.caffeinemc.sodium.util.packed.ColorABGR;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;
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

        float v1x = Matrix4fUtil.transformVectorX(position, x1f, y1f, z1f);
        float v1y = Matrix4fUtil.transformVectorY(position, x1f, y1f, z1f);
        float v1z = Matrix4fUtil.transformVectorZ(position, x1f, y1f, z1f);
        
        float v2x = Matrix4fUtil.transformVectorX(position, x2f, y1f, z1f);
        float v2y = Matrix4fUtil.transformVectorY(position, x2f, y1f, z1f);
        float v2z = Matrix4fUtil.transformVectorZ(position, x2f, y1f, z1f);
        
        float v3x = Matrix4fUtil.transformVectorX(position, x1f, y2f, z1f);
        float v3y = Matrix4fUtil.transformVectorY(position, x1f, y2f, z1f);
        float v3z = Matrix4fUtil.transformVectorZ(position, x1f, y2f, z1f);
        
        float v4x = Matrix4fUtil.transformVectorX(position, x1f, y1f, z2f);
        float v4y = Matrix4fUtil.transformVectorY(position, x1f, y1f, z2f);
        float v4z = Matrix4fUtil.transformVectorZ(position, x1f, y1f, z2f);
        
        float v5x = Matrix4fUtil.transformVectorX(position, x2f, y2f, z1f);
        float v5y = Matrix4fUtil.transformVectorY(position, x2f, y2f, z1f);
        float v5z = Matrix4fUtil.transformVectorZ(position, x2f, y2f, z1f);
        
        float v6x = Matrix4fUtil.transformVectorX(position, x1f, y2f, z2f);
        float v6y = Matrix4fUtil.transformVectorY(position, x1f, y2f, z2f);
        float v6z = Matrix4fUtil.transformVectorZ(position, x1f, y2f, z2f);
        
        float v7x = Matrix4fUtil.transformVectorX(position, x2f, y1f, z2f);
        float v7y = Matrix4fUtil.transformVectorY(position, x2f, y1f, z2f);
        float v7z = Matrix4fUtil.transformVectorZ(position, x2f, y1f, z2f);
        
        float v8x = Matrix4fUtil.transformVectorX(position, x2f, y2f, z2f);
        float v8y = Matrix4fUtil.transformVectorY(position, x2f, y2f, z2f);
        float v8z = Matrix4fUtil.transformVectorZ(position, x2f, y2f, z2f);

        LineVertexSink lines = VertexDrain.of(vertexConsumer)
                .createSink(VanillaVertexFormats.LINES);
        lines.ensureCapacity(24);

        lines.vertexLine(v1x, v1y, v1z, ColorABGR.pack(red, yAxisGreen, zAxisBlue, alpha), Normal3b.pack(normal.a00, normal.a10, normal.a20));
        lines.vertexLine(v2x, v2y, v2z, ColorABGR.pack(red, yAxisGreen, zAxisBlue, alpha), Normal3b.pack(normal.a00, normal.a10, normal.a20));

        lines.vertexLine(v1x, v1y, v1z, ColorABGR.pack(xAxisRed, green, zAxisBlue, alpha), Normal3b.pack(normal.a01, normal.a11, normal.a21));
        lines.vertexLine(v3x, v3y, v3z, ColorABGR.pack(xAxisRed, green, zAxisBlue, alpha), Normal3b.pack(normal.a01, normal.a11, normal.a21));

        lines.vertexLine(v1x, v1y, v1z, ColorABGR.pack(xAxisRed, yAxisGreen, blue, alpha), Normal3b.pack(normal.a02, normal.a12, normal.a22));
        lines.vertexLine(v4x, v4y, v4z, ColorABGR.pack(xAxisRed, yAxisGreen, blue, alpha), Normal3b.pack(normal.a02, normal.a12, normal.a22));

        lines.vertexLine(v2x, v2y, v2z, color, Normal3b.pack(normal.a01, normal.a11, normal.a21));
        lines.vertexLine(v5x, v5y, v5z, color, Normal3b.pack(normal.a01, normal.a11, normal.a21));

        lines.vertexLine(v5x, v5y, v5z, color, Normal3b.pack(-normal.a00, -normal.a10, -normal.a20));
        lines.vertexLine(v3x, v3y, v3z, color, Normal3b.pack(-normal.a00, -normal.a10, -normal.a20));

        lines.vertexLine(v3x, v3y, v3z, color, Normal3b.pack(normal.a02, normal.a12, normal.a22));
        lines.vertexLine(v6x, v6y, v6z, color, Normal3b.pack(normal.a02, normal.a12, normal.a22));

        lines.vertexLine(v6x, v6y, v6z, color, Normal3b.pack(-normal.a01, -normal.a11, -normal.a21));
        lines.vertexLine(v4x, v4y, v4z, color, Normal3b.pack(-normal.a01, -normal.a11, -normal.a21));

        lines.vertexLine(v4x, v4y, v4z, color, Normal3b.pack(normal.a00, normal.a10, normal.a20));
        lines.vertexLine(v7x, v7y, v7z, color, Normal3b.pack(normal.a00, normal.a10, normal.a20));

        lines.vertexLine(v7x, v7y, v7z, color, Normal3b.pack(-normal.a02, -normal.a12, -normal.a22));
        lines.vertexLine(v2x, v2y, v2z, color, Normal3b.pack(-normal.a02, -normal.a12, -normal.a22));

        lines.vertexLine(v6x, v6y, v6z, color, Normal3b.pack(normal.a00, normal.a10, normal.a20));
        lines.vertexLine(v8x, v8y, v8z, color, Normal3b.pack(normal.a00, normal.a10, normal.a20));

        lines.vertexLine(v7x, v7y, v7z, color, Normal3b.pack(normal.a01, normal.a11, normal.a21));
        lines.vertexLine(v8x, v8y, v8z, color, Normal3b.pack(normal.a01, normal.a11, normal.a21));

        lines.vertexLine(v5x, v5y, v5z, color, Normal3b.pack(normal.a02, normal.a12, normal.a22));
        lines.vertexLine(v8x, v8y, v8z, color, Normal3b.pack(normal.a02, normal.a12, normal.a22));

        lines.flush();
    }

}
