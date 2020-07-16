package me.jellysquid.mods.sodium.mixin.features.buffer_builder.intrinsics;

import me.jellysquid.mods.sodium.client.model.consumer.ParticleVertexConsumer;
import me.jellysquid.mods.sodium.client.model.consumer.QuadVertexConsumer;
import net.minecraft.client.render.SpriteTexturedVertexConsumer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.Sprite;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SpriteTexturedVertexConsumer.class)
public abstract class MixinSpriteTexturedVertexConsumer implements QuadVertexConsumer, ParticleVertexConsumer {
    @Shadow
    @Final
    private VertexConsumer parent;

    @Shadow
    @Final
    private Sprite sprite;

    @Override
    public void vertexQuad(float x, float y, float z, int color, float u, float v, int light, int overlay, int norm) {
        u = this.sprite.getFrameU(u * 16.0F);
        v = this.sprite.getFrameV(v * 16.0F);

        ((QuadVertexConsumer) this.parent).vertexQuad(x, y, z, color, u, v, light, overlay, norm);
    }

    @Override
    public void vertexParticle(float x, float y, float z, float u, float v, int color, int light) {
        u = this.sprite.getFrameU(u * 16.0F);
        v = this.sprite.getFrameV(v * 16.0F);

        ((ParticleVertexConsumer) this.parent).vertexParticle(x, y, z, u, v, color, light);
    }

}
