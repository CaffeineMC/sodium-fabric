package me.jellysquid.mods.sodium.mixin.features.buffer_builder.intrinsics;

import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.render.immediate.model.BakedModelEncoder;
import me.jellysquid.mods.sodium.client.render.texture.SpriteUtil;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
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
    private boolean canSkipElementChecks;

    @Override
    public void quad(MatrixStack.Entry matrices, BakedQuad bakedQuad, float r, float g, float b, int light, int overlay) {
        if (!this.canSkipElementChecks) {
            super.quad(matrices, bakedQuad, r, g, b, light, overlay);

            SpriteUtil.markSpriteActive(bakedQuad.getSprite());

            return;
        }

        if (this.colorFixed) {
            throw new IllegalStateException();
        }

        VertexBufferWriter writer = VertexBufferWriter.of(this);

        ModelQuadView quad = (ModelQuadView) bakedQuad;

        int color = ColorABGR.pack(r, g, b, 1.0F);
        BakedModelEncoder.writeQuadVertices(writer, matrices, quad, color, light, overlay);

        SpriteUtil.markSpriteActive(quad.getSprite());
    }

    @Override
    public void quad(MatrixStack.Entry matrices, BakedQuad bakedQuad, float[] brightnessTable, float r, float g, float b, int[] light, int overlay, boolean colorize) {
        if (!this.canSkipElementChecks) {
            super.quad(matrices, bakedQuad, brightnessTable, r, g, b, light, overlay, colorize);

            SpriteUtil.markSpriteActive(bakedQuad.getSprite());

            return;
        }

        if (this.colorFixed) {
            throw new IllegalStateException();
        }

        VertexBufferWriter writer = VertexBufferWriter.of(this);

        ModelQuadView quad = (ModelQuadView) bakedQuad;

        BakedModelEncoder.writeQuadVertices(writer, matrices, quad, r, g, b, brightnessTable, colorize, light, overlay);

        SpriteUtil.markSpriteActive(quad.getSprite());
    }
}
