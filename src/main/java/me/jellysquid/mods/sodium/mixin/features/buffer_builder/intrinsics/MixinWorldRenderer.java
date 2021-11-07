package me.jellysquid.mods.sodium.mixin.features.buffer_builder.intrinsics;

import me.jellysquid.mods.sodium.client.model.vertex.VanillaVertexTypes;
import me.jellysquid.mods.sodium.client.model.vertex.VertexDrain;
import me.jellysquid.mods.sodium.client.model.vertex.formats.line.LineVertexSink;
import me.jellysquid.mods.sodium.client.util.Norm3b;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import me.jellysquid.mods.sodium.client.util.math.Matrix3fExtended;
import me.jellysquid.mods.sodium.client.util.math.Matrix4fExtended;
import me.jellysquid.mods.sodium.client.util.math.MatrixUtil;
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

        Matrix4fExtended positionExt = MatrixUtil.getExtendedMatrix(position);
        Matrix3fExtended normalExt = MatrixUtil.getExtendedMatrix(normal);

        float v1x = positionExt.transformVecX(x1f, y1f, z1f);
        float v1y = positionExt.transformVecY(x1f, y1f, z1f);
        float v1z = positionExt.transformVecZ(x1f, y1f, z1f);
        
        float v2x = positionExt.transformVecX(x2f, y1f, z1f);
        float v2y = positionExt.transformVecY(x2f, y1f, z1f);
        float v2z = positionExt.transformVecZ(x2f, y1f, z1f);
        
        float v3x = positionExt.transformVecX(x1f, y2f, z1f);
        float v3y = positionExt.transformVecY(x1f, y2f, z1f);
        float v3z = positionExt.transformVecZ(x1f, y2f, z1f);
        
        float v4x = positionExt.transformVecX(x1f, y1f, z2f);
        float v4y = positionExt.transformVecY(x1f, y1f, z2f);
        float v4z = positionExt.transformVecZ(x1f, y1f, z2f);
        
        float v5x = positionExt.transformVecX(x2f, y2f, z1f);
        float v5y = positionExt.transformVecY(x2f, y2f, z1f);
        float v5z = positionExt.transformVecZ(x2f, y2f, z1f);
        
        float v6x = positionExt.transformVecX(x1f, y2f, z2f);
        float v6y = positionExt.transformVecY(x1f, y2f, z2f);
        float v6z = positionExt.transformVecZ(x1f, y2f, z2f);
        
        float v7x = positionExt.transformVecX(x2f, y1f, z2f);
        float v7y = positionExt.transformVecY(x2f, y1f, z2f);
        float v7z = positionExt.transformVecZ(x2f, y1f, z2f);
        
        float v8x = positionExt.transformVecX(x2f, y2f, z2f);
        float v8y = positionExt.transformVecY(x2f, y2f, z2f);
        float v8z = positionExt.transformVecZ(x2f, y2f, z2f);

        LineVertexSink lines = VertexDrain.of(vertexConsumer)
                .createSink(VanillaVertexTypes.LINES);
        lines.ensureCapacity(24);

        lines.vertexLine(v1x, v1y, v1z, ColorABGR.pack(red, yAxisGreen, zAxisBlue, alpha), Norm3b.pack(normalExt.getA00(), normalExt.getA10(), normalExt.getA20()));
        lines.vertexLine(v2x, v2y, v2z, ColorABGR.pack(red, yAxisGreen, zAxisBlue, alpha), Norm3b.pack(normalExt.getA00(), normalExt.getA10(), normalExt.getA20()));

        lines.vertexLine(v1x, v1y, v1z, ColorABGR.pack(xAxisRed, green, zAxisBlue, alpha), Norm3b.pack(normalExt.getA01(), normalExt.getA11(), normalExt.getA21()));
        lines.vertexLine(v3x, v3y, v3z, ColorABGR.pack(xAxisRed, green, zAxisBlue, alpha), Norm3b.pack(normalExt.getA01(), normalExt.getA11(), normalExt.getA21()));

        lines.vertexLine(v1x, v1y, v1z, ColorABGR.pack(xAxisRed, yAxisGreen, blue, alpha), Norm3b.pack(normalExt.getA02(), normalExt.getA12(), normalExt.getA22()));
        lines.vertexLine(v4x, v4y, v4z, ColorABGR.pack(xAxisRed, yAxisGreen, blue, alpha), Norm3b.pack(normalExt.getA02(), normalExt.getA12(), normalExt.getA22()));

        lines.vertexLine(v2x, v2y, v2z, color, Norm3b.pack(normalExt.getA01(), normalExt.getA11(), normalExt.getA21()));
        lines.vertexLine(v5x, v5y, v5z, color, Norm3b.pack(normalExt.getA01(), normalExt.getA11(), normalExt.getA21()));

        lines.vertexLine(v5x, v5y, v5z, color, Norm3b.pack(-normalExt.getA00(), -normalExt.getA10(), -normalExt.getA20()));
        lines.vertexLine(v3x, v3y, v3z, color, Norm3b.pack(-normalExt.getA00(), -normalExt.getA10(), -normalExt.getA20()));

        lines.vertexLine(v3x, v3y, v3z, color, Norm3b.pack(normalExt.getA02(), normalExt.getA12(), normalExt.getA22()));
        lines.vertexLine(v6x, v6y, v6z, color, Norm3b.pack(normalExt.getA02(), normalExt.getA12(), normalExt.getA22()));

        lines.vertexLine(v6x, v6y, v6z, color, Norm3b.pack(-normalExt.getA01(), -normalExt.getA11(), -normalExt.getA21()));
        lines.vertexLine(v4x, v4y, v4z, color, Norm3b.pack(-normalExt.getA01(), -normalExt.getA11(), -normalExt.getA21()));

        lines.vertexLine(v4x, v4y, v4z, color, Norm3b.pack(normalExt.getA00(), normalExt.getA10(), normalExt.getA20()));
        lines.vertexLine(v7x, v7y, v7z, color, Norm3b.pack(normalExt.getA00(), normalExt.getA10(), normalExt.getA20()));

        lines.vertexLine(v7x, v7y, v7z, color, Norm3b.pack(-normalExt.getA02(), -normalExt.getA12(), -normalExt.getA22()));
        lines.vertexLine(v2x, v2y, v2z, color, Norm3b.pack(-normalExt.getA02(), -normalExt.getA12(), -normalExt.getA22()));

        lines.vertexLine(v6x, v6y, v6z, color, Norm3b.pack(normalExt.getA00(), normalExt.getA10(), normalExt.getA20()));
        lines.vertexLine(v8x, v8y, v8z, color, Norm3b.pack(normalExt.getA00(), normalExt.getA10(), normalExt.getA20()));

        lines.vertexLine(v7x, v7y, v7z, color, Norm3b.pack(normalExt.getA01(), normalExt.getA11(), normalExt.getA21()));
        lines.vertexLine(v8x, v8y, v8z, color, Norm3b.pack(normalExt.getA01(), normalExt.getA11(), normalExt.getA21()));

        lines.vertexLine(v5x, v5y, v5z, color, Norm3b.pack(normalExt.getA02(), normalExt.getA12(), normalExt.getA22()));
        lines.vertexLine(v8x, v8y, v8z, color, Norm3b.pack(normalExt.getA02(), normalExt.getA12(), normalExt.getA22()));

        lines.flush();
    }

}
