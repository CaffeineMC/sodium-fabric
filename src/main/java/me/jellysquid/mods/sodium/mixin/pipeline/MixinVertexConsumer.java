package me.jellysquid.mods.sodium.mixin.pipeline;

import me.jellysquid.mods.sodium.client.model.ParticleVertexConsumer;
import me.jellysquid.mods.sodium.client.util.ColorARGB;
import net.minecraft.client.render.VertexConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(VertexConsumer.class)
public interface MixinVertexConsumer extends ParticleVertexConsumer {
    @Shadow
    VertexConsumer vertex(double x, double y, double z);

    @Shadow
    VertexConsumer texture(float u, float v);

    @Shadow
    VertexConsumer color(int red, int green, int blue, int alpha);

    @Shadow
    VertexConsumer light(int uv);

    @Shadow
    void next();

    @Override
    default void vertexParticle(float x, float y, float z, float u, float v, int color, int light) {
        this.vertex(x, y, z);
        this.texture(u, v);
        this.color(ColorARGB.unpackRed(color), ColorARGB.unpackGreen(color), ColorARGB.unpackBlue(color), ColorARGB.unpackAlpha(color));
        this.light(light);
        this.next();
    }
}
