package me.jellysquid.mods.sodium.mixin.features.buffer_builder.intrinsics;

import me.jellysquid.mods.sodium.interop.vanilla.matrix.Matrix3fUtil;
import me.jellysquid.mods.sodium.interop.vanilla.quad.BakedQuadView;
import me.jellysquid.mods.sodium.model.vertex.VanillaVertexTypes;
import me.jellysquid.mods.sodium.model.vertex.VertexDrain;
import me.jellysquid.mods.sodium.model.vertex.formats.ModelQuadVertexSink;
import me.jellysquid.mods.sodium.util.DirectionUtil;
import me.jellysquid.mods.sodium.util.color.ColorABGR;
import me.jellysquid.mods.sodium.util.color.ColorU8;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.FixedColorVertexConsumer;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@SuppressWarnings({ "SameParameterValue" })
@Mixin(BufferBuilder.class)
public abstract class MixinBufferBuilder extends FixedColorVertexConsumer {
    @Shadow
    private boolean textured;

    @Override
    public void quad(MatrixStack.Entry matrices, BakedQuad quad, float[] brightnessTable, float r, float g, float b, int[] light, int overlay, boolean colorize) {
        if (!this.textured) {
            super.quad(matrices, quad, brightnessTable, r, g, b, light, overlay, colorize);
            return;
        }

        if (this.colorFixed) {
            throw new IllegalStateException();
        }

        BakedQuadView q = (BakedQuadView) quad;

        ModelQuadVertexSink drain = VertexDrain.of(this)
                .createSink(VanillaVertexTypes.QUADS);
        drain.ensureCapacity(4);

        for (int i = 0; i < 4; i++) {
            float x = q.getX(i);
            float y = q.getY(i);
            float z = q.getZ(i);

            float brightness = brightnessTable[i];
            int color = ColorABGR.mul(colorize ? q.getColor(i) : 0xFFFFFFFF, brightness * r, brightness * g, brightness * b);

            float u = q.getTexU(i);
            float v = q.getTexV(i);

            drain.writeQuad(matrices, x, y, z, color, u, v, light[i], overlay,
                    DirectionUtil.getOpposite(quad.getFace()));
        }

        drain.flush();
    }
}
