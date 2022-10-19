package me.jellysquid.mods.sodium.mixin.features.texture_tracking;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.render.texture.SpriteExtended;
import net.minecraft.class_7764;
import net.minecraft.client.texture.Sprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(class_7764.class_7765.class)
public class MixinSpriteAnimation {
    @Unique
    private class_7764 parent;

    /**
     * @author IMS
     * @reason Replace fragile Shadow
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    public void assignParent(class_7764 parent, class_7764.Animation animation, class_7764.Interpolation interpolation, CallbackInfo ci) {
        this.parent = parent;
    }

    @Inject(method = "method_45824", at = @At("HEAD"), cancellable = true)
    private void preTick(CallbackInfo ci) {
        SpriteExtended parent = (SpriteExtended) this.parent;

        boolean onDemand = SodiumClientMod.options().performance.animateOnlyVisibleTextures;

        if (onDemand && !parent.isActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "method_45824", at = @At("TAIL"))
    private void postTick(CallbackInfo ci) {
        SpriteExtended parent = (SpriteExtended) this.parent;
        parent.setActive(false);
    }
}
