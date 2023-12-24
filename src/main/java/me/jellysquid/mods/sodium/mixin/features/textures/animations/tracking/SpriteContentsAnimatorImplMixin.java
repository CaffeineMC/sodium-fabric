package me.jellysquid.mods.sodium.mixin.features.textures.animations.tracking;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.render.texture.SpriteContentsExtended;
import net.minecraft.client.texture.SpriteContents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(SpriteContents.AnimatorImpl.class)
public class SpriteContentsAnimatorImplMixin {
    @Shadow
    int currentTime;
    @Shadow
    @Final
    SpriteContents.Animation animation;
    @Shadow
    int frame;

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
        SpriteContentsExtended parent = (SpriteContentsExtended) this.parent;

        boolean onDemand = SodiumClientMod.options().performance.animateOnlyVisibleTextures;

        if (onDemand && !parent.sodium$isActive()) {
            this.currentTime++;
            List<SpriteContents.AnimationFrame> frames = ((SpriteContentsAnimationAccessor)this.animation).getFrames();
            if (this.currentTime >= ((SpriteContentsAnimationFrameAccessor)frames.get(this.frame)).getTime()) {
                this.frame = (this.frame + 1) % frames.size();
                this.currentTime = 0;
            }
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void postTick(CallbackInfo ci) {
        SpriteContentsExtended parent = (SpriteContentsExtended) this.parent;
        parent.sodium$setActive(false);
    }
}
