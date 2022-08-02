package net.caffeinemc.sodium.mixin.features.buffer_builder.intrinsics;

import net.caffeinemc.sodium.interop.vanilla.vertex.VanillaVertexFormats;
import net.caffeinemc.sodium.interop.vanilla.vertex.formats.ModelQuadVertexSink;
import net.caffeinemc.sodium.render.terrain.quad.ModelQuadView;
import net.caffeinemc.sodium.render.vertex.VertexDrain;
import net.caffeinemc.sodium.util.DirectionUtil;
import net.caffeinemc.sodium.util.packed.ColorABGR;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.FixedColorVertexConsumer;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
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

        ModelQuadView q = (ModelQuadView) quad;

        ModelQuadVertexSink drain = VertexDrain.of(this)
                .createSink(VanillaVertexFormats.QUADS);
        drain.ensureCapacity(4);

        for (int i = 0; i < 4; i++) {
            float x = q.getX(i);
            float y = q.getY(i);
            float z = q.getZ(i);

            float brightness = brightnessTable[i];
            int color = ColorABGR.mul(colorize ? q.getColor(i) : 0xFFFFFFFF, brightness * r, brightness * g, brightness * b);

            float u = q.getTexU(i);
            float v = q.getTexV(i);

            drain.writeQuad(matrices, x, y, z, color, u, v, light[i], overlay, quad.getFace());
        }

        drain.flush();
    }
}
