package me.jellysquid.mods.sodium.mixin.features.render.gui.font;

import me.jellysquid.mods.sodium.client.render.vertex.VertexConsumerUtils;
import net.caffeinemc.mods.sodium.api.vertex.format.common.GlyphVertex;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.caffeinemc.mods.sodium.api.math.MatrixHelper;
import net.minecraft.client.font.GlyphRenderer;
import net.minecraft.client.render.VertexConsumer;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlyphRenderer.class)
public class GlyphRendererMixin {
    @Shadow
    @Final
    private float minX;

    @Shadow
    @Final
    private float maxX;

    @Shadow
    @Final
    private float minY;

    @Shadow
    @Final
    private float maxY;

    @Shadow
    @Final
    private float minU;

    @Shadow
    @Final
    private float minV;

    @Shadow
    @Final
    private float maxV;

    @Shadow
    @Final
    private float maxU;

    /**
     * @reason Use intrinsics
     * @author JellySquid
     */
    @Inject(method = "draw", at = @At("HEAD"), cancellable = true)
    private void drawFast(boolean italic, float x, float y, Matrix4f matrix, VertexConsumer vertexConsumer, float red, float green, float blue, float alpha, int light, CallbackInfo ci) {
        var writer = VertexConsumerUtils.convertOrLog(vertexConsumer);

        if (writer == null) {
            return;
        }

        ci.cancel();

        float x1 = x + this.minX;
        float x2 = x + this.maxX;
        float y1 = y + this.minY;
        float y2 = y + this.maxY;
        float w1 = italic ? 1.0F - 0.25F * y1 : 0.0F;
        float w2 = italic ? 1.0F - 0.25F * y2 : 0.0F;

        int color = ColorABGR.pack(red, green, blue, alpha);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            long buffer = stack.nmalloc(4 * GlyphVertex.STRIDE);
            long ptr = buffer;

            write(ptr, matrix, x1 + w1, y1, 0.0F, color, this.minU, this.minV, light);
            ptr += GlyphVertex.STRIDE;

            write(ptr, matrix, x1 + w2, y2, 0.0F, color, this.minU, this.maxV, light);
            ptr += GlyphVertex.STRIDE;

            write(ptr, matrix, x2 + w2, y2, 0.0F, color, this.maxU, this.maxV, light);
            ptr += GlyphVertex.STRIDE;

            write(ptr, matrix, x2 + w1, y1, 0.0F, color, this.maxU, this.minV, light);
            ptr += GlyphVertex.STRIDE;

            writer.push(stack, buffer, 4, GlyphVertex.FORMAT);
        }
    }

    @Unique
    private static void write(long buffer,
                              Matrix4f matrix, float x, float y, float z, int color, float u, float v, int light) {
        float x2 = MatrixHelper.transformPositionX(matrix, x, y, z);
        float y2 = MatrixHelper.transformPositionY(matrix, x, y, z);
        float z2 = MatrixHelper.transformPositionZ(matrix, x, y, z);

        GlyphVertex.put(buffer, x2, y2, z2, color, u, v, light);
    }

}
