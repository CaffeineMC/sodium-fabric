package me.jellysquid.mods.sodium.mixin.pipeline;

import me.jellysquid.mods.sodium.client.model.consumer.GlyphVertexConsumer;
import me.jellysquid.mods.sodium.client.model.consumer.ParticleVertexConsumer;
import me.jellysquid.mods.sodium.client.model.consumer.QuadVertexConsumer;
import me.jellysquid.mods.sodium.client.util.Norm3b;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(VertexConsumer.class)
public interface MixinVertexConsumer extends ParticleVertexConsumer, QuadVertexConsumer, GlyphVertexConsumer {
    @Shadow
    VertexConsumer vertex(double x, double y, double z);

    @Shadow
    VertexConsumer texture(float u, float v);

    @Shadow
    VertexConsumer color(int red, int green, int blue, int alpha);

    @Shadow
    VertexConsumer light(int uv);

    @Shadow
    VertexConsumer overlay(int uv);

    @Shadow
    VertexConsumer normal(float x, float y, float z);

    @Shadow
    void next();

    @Override
    default void vertexParticle(float x, float y, float z, float u, float v, int color, int light) {
        this.vertex(x, y, z);
        this.texture(u, v);
        this.color(ColorABGR.unpackRed(color), ColorABGR.unpackGreen(color), ColorABGR.unpackBlue(color), ColorABGR.unpackAlpha(color));
        this.light(light);
        this.next();
    }

    @Override
    default void vertexQuad(float x, float y, float z, int color, float u, float v, int light, int overlay, int normal) {
        this.vertex(x, y, z);
        this.color(ColorABGR.unpackRed(color), ColorABGR.unpackGreen(color), ColorABGR.unpackBlue(color), ColorABGR.unpackAlpha(color));
        this.texture(u, v);
        this.overlay(overlay);
        this.light(light);
        this.normal(Norm3b.unpackX(normal), Norm3b.unpackY(normal), Norm3b.unpackZ(normal));
        this.next();
    }

    @Override
    default void vertexGlyph(Matrix4f matrix, float x, float y, float z, int color, float u, float v, int light) {
        this.vertex(x, y, z);
        this.color(ColorABGR.unpackRed(color), ColorABGR.unpackGreen(color), ColorABGR.unpackBlue(color), ColorABGR.unpackAlpha(color));
        this.texture(u, v);
        this.light(light);
        this.next();
    }
}
