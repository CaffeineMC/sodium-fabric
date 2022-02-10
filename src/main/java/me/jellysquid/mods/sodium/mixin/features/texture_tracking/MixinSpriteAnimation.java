package me.jellysquid.mods.sodium.mixin.features.texture_tracking;

import me.jellysquid.mods.sodium.SodiumClientMod;
import me.jellysquid.mods.sodium.interop.vanilla.mixin.SpriteVisibilityStorage;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(TextureAtlasSprite.AnimatedTexture.class)
public class MixinSpriteAnimation {
    @Unique
    private TextureAtlasSprite parent;

    /**
     * @author IMS
     * @reason Replace fragile Shadow
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    public void assignParent(TextureAtlasSprite parent, List frames, int frameCount, TextureAtlasSprite.InterpolationData interpolation, CallbackInfo ci) {
        this.parent = parent;
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void preTick(CallbackInfo ci) {
        SpriteVisibilityStorage parent = (SpriteVisibilityStorage) this.parent;

        boolean onDemand = SodiumClientMod.options().performance.animateOnlyVisibleTextures;

        if (onDemand && !parent.isActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void postTick(CallbackInfo ci) {
        SpriteVisibilityStorage parent = (SpriteVisibilityStorage) this.parent;
        parent.setActive(false);
    }
}
