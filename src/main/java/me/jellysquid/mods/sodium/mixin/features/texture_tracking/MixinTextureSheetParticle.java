package me.jellysquid.mods.sodium.mixin.features.texture_tracking;

import com.mojang.blaze3d.vertex.VertexConsumer;
import me.jellysquid.mods.sodium.client.render.texture.SpriteUtil;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextureSheetParticle.class)
public abstract class MixinTextureSheetParticle extends SingleQuadParticle {
    @Shadow
    protected TextureAtlasSprite sprite;

    private boolean shouldTickSprite;

    protected MixinTextureSheetParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
    }

    @Inject(method = "setSprite", at = @At("RETURN"))
    private void afterSetSprite(TextureAtlasSprite textureAtlasSprite, CallbackInfo ci) {
        this.shouldTickSprite = sprite != null && sprite.getAnimationTicker() != null;
    }

    @Override
    public void render(VertexConsumer vertexConsumer, Camera camera, float tickDelta) {
        if (this.shouldTickSprite) {
            SpriteUtil.markSpriteActive(this.sprite);
        }

        super.render(vertexConsumer, camera, tickDelta);
    }
}