package me.jellysquid.mods.sodium.mixin.features.texture_tracking;

import me.jellysquid.mods.sodium.client.render.texture.SpriteExtended;
import me.jellysquid.mods.sodium.client.render.texture.SpriteUtil;
import net.minecraft.client.particle.BillboardParticle;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpriteBillboardParticle.class)
public abstract class MixinSpriteBillboardParticle extends BillboardParticle {
    @Shadow
    protected Sprite sprite;

    private boolean shouldTickSprite;

    protected MixinSpriteBillboardParticle(ClientWorld world, double x, double y, double z) {
        super(world, x, y, z);
    }

    @Inject(method = "setSprite(Lnet/minecraft/client/texture/Sprite;)V", at = @At("RETURN"))
    private void afterSetSprite(Sprite sprite, CallbackInfo ci) {
        this.shouldTickSprite = sprite != null && ((SpriteExtended) sprite.getContents()).hasAnimation();
    }

    @Override
    public void buildGeometry(VertexConsumer vertexConsumer, Camera camera, float tickDelta) {
        if (this.shouldTickSprite) {
            SpriteUtil.markSpriteActive(this.sprite);
        }

        super.buildGeometry(vertexConsumer, camera, tickDelta);
    }
}