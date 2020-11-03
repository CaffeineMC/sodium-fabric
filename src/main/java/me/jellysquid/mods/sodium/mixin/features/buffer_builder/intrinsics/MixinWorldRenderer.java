package me.jellysquid.mods.sodium.mixin.features.buffer_builder.intrinsics;

import me.jellysquid.mods.sodium.client.model.consumer.LineVertexConsumer;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
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
        Matrix4f model = matrices.peek().getModel();

        float x1f = (float) x1;
        float y1f = (float) y1;
        float z1f = (float) z1;
        float x2f = (float) x2;
        float y2f = (float) y2;
        float z2f = (float) z2;

        int color = ColorABGR.pack(red, green, blue, alpha);

        LineVertexConsumer lines = (LineVertexConsumer) vertexConsumer;
        lines.vertexLine(model, x1f, y1f, z1f, red, yAxisGreen, zAxisBlue, alpha);
        lines.vertexLine(model, x2f, y1f, z1f, red, yAxisGreen, zAxisBlue, alpha);
        lines.vertexLine(model, x1f, y1f, z1f, xAxisRed, green, zAxisBlue, alpha);
        lines.vertexLine(model, x1f, y2f, z1f, xAxisRed, green, zAxisBlue, alpha);
        lines.vertexLine(model, x1f, y1f, z1f, xAxisRed, yAxisGreen, blue, alpha);
        lines.vertexLine(model, x1f, y1f, z2f, xAxisRed, yAxisGreen, blue, alpha);
        lines.vertexLine(model, x2f, y1f, z1f, color);
        lines.vertexLine(model, x2f, y2f, z1f, color);
        lines.vertexLine(model, x2f, y2f, z1f, color);
        lines.vertexLine(model, x1f, y2f, z1f, color);
        lines.vertexLine(model, x1f, y2f, z1f, color);
        lines.vertexLine(model, x1f, y2f, z2f, color);
        lines.vertexLine(model, x1f, y2f, z2f, color);
        lines.vertexLine(model, x1f, y1f, z2f, color);
        lines.vertexLine(model, x1f, y1f, z2f, color);
        lines.vertexLine(model, x2f, y1f, z2f, color);
        lines.vertexLine(model, x2f, y1f, z2f, color);
        lines.vertexLine(model, x2f, y1f, z1f, color);
        lines.vertexLine(model, x1f, y2f, z2f, color);
        lines.vertexLine(model, x2f, y2f, z2f, color);
        lines.vertexLine(model, x2f, y1f, z2f, color);
        lines.vertexLine(model, x2f, y2f, z2f, color);
        lines.vertexLine(model, x2f, y2f, z1f, color);
        lines.vertexLine(model, x2f, y2f, z2f, color);
    }

}
