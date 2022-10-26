package me.jellysquid.mods.sodium.mixin.features.texture_tracking;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.render.texture.SpriteExtended;
import net.minecraft.client.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpriteContents.AnimatorImpl.class)
public class MixinSpriteContentsAnimatorImpl {
    @Unique
    private SpriteContents parent;

    /**
     * @author IMS
     * @reason Replace fragile Shadow
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    public void assignParent(SpriteContents spriteContents, SpriteContents.Animation animation, SpriteContents.Interpolation interpolation, CallbackInfo ci) {
        this.parent = spriteContents;
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void preTick(CallbackInfo ci) {
        SpriteExtended parent = (SpriteExtended) this.parent;

        boolean onDemand = SodiumClientMod.options().performance.animateOnlyVisibleTextures;

        if (onDemand && !parent.isActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void postTick(CallbackInfo ci) {
        SpriteExtended parent = (SpriteExtended) this.parent;
        parent.setActive(false);
    }
}
