package me.jellysquid.mods.sodium.mixin.features.texture_tracking;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.render.texture.SpriteExtended;
import net.minecraft.client.texture.Sprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Sprite.Animation.class)
public class MixinSpriteAnimation implements SpriteExtended {
    private boolean forceNextUpdate;

    /**
     * @author JellySquid & FlashyReese
     * @reason Allow conditional texture updating
     */
    @Inject(method = "tick", at = @At(value = "HEAD"), cancellable = true)
    public void tick(CallbackInfo callbackInfo) {
        boolean onDemand = SodiumClientMod.options().advanced.animateOnlyVisibleTextures;

        if (onDemand && !this.forceNextUpdate) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "tick", at = @At(value = "TAIL"))
    public void tickTail(CallbackInfo callbackInfo) {
        this.forceNextUpdate = false;
    }

    @Override
    public void markActive() {
        this.forceNextUpdate = true;
    }
}