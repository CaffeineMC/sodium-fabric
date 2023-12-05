package me.jellysquid.mods.sodium.mixin.debug.checks;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.PlayerSkinTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerSkinTexture.class)
public abstract class PlayerSkinTextureMixin {
    @Shadow
    private boolean loaded;

    @Shadow
    protected abstract void uploadTexture(NativeImage image);

    @Inject(method = "onTextureLoaded", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;execute(Ljava/lang/Runnable;)V", shift = At.Shift.BEFORE), cancellable = true)
    private void validateCurrentThread$loadTextureCallback(NativeImage image, CallbackInfo ci) {
        MinecraftClient.getInstance().execute(() -> {
            this.loaded = true;
            this.uploadTexture(image);
        });

        ci.cancel();
    }
}
