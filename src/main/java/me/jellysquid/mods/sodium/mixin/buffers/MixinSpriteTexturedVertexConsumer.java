package me.jellysquid.mods.sodium.mixin.buffers;

import me.jellysquid.mods.sodium.client.render.pipeline.DirectVertexConsumer;
import net.minecraft.client.render.SpriteTexturedVertexConsumer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.Sprite;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SpriteTexturedVertexConsumer.class)
public abstract class MixinSpriteTexturedVertexConsumer implements DirectVertexConsumer {
    @Shadow
    @Final
    private VertexConsumer parent;

    @Shadow
    @Final
    private Sprite sprite;

    @Override
    public void vertex(float x, float y, float z, int color, float u, float v, int overlay, int light, int norm) {
        if (!(this.parent instanceof DirectVertexConsumer)) {
            throw new UnsupportedOperationException("Not a direct buffer");
        }

        u = this.sprite.getFrameU(u * 16.0F);
        v = this.sprite.getFrameV(v * 16.0F);

        ((DirectVertexConsumer) this.parent).vertex(x, y, z, color, u, v, overlay, light, norm);
    }

    @Override
    public void vertexParticle(float x, float y, float z, float u, float v, int color, int light) {
        if (!(this.parent instanceof DirectVertexConsumer)) {
            throw new UnsupportedOperationException("Not a direct buffer");
        }

        u = this.sprite.getFrameU(u * 16.0F);
        v = this.sprite.getFrameV(v * 16.0F);

        ((DirectVertexConsumer) this.parent).vertexParticle(x, y, z, u, v, color, light);
    }
}
