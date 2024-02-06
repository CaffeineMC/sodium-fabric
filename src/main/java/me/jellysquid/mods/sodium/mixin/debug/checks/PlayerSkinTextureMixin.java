package me.jellysquid.mods.sodium.mixin.debug.checks;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.HttpTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HttpTexture.class)
public abstract class PlayerSkinTextureMixin {
    @Shadow
    private boolean uploaded;

    @Shadow
    protected abstract void upload(NativeImage image);

    @Inject(method = "loadCallback", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;execute(Ljava/lang/Runnable;)V", shift = At.Shift.BEFORE), cancellable = true)
    private void validateCurrentThread$loadTextureCallback(NativeImage image, CallbackInfo ci) {
        Minecraft.getInstance().execute(() -> {
            this.uploaded = true;
            this.upload(image);
        });

        ci.cancel();
    }
}
