package net.caffeinemc.mods.sodium.mixin.features.render.compositing;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.caffeinemc.mods.sodium.client.render.util.RenderTargetTracker;
import net.minecraft.client.renderer.LevelRenderer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Shadow
    @Nullable
    private RenderTarget entityTarget;

    @Inject(method = "doEntityOutline", at = @At("HEAD"), cancellable = true)
    private void preEntityOutlineComposite(CallbackInfo ci) {
        RenderTarget entityTarget = this.entityTarget;

        // If the entity render target hasn't been modified, don't try to composite it into the final image
        if (entityTarget != null && !RenderTargetTracker.isDirty(entityTarget)) {
            ci.cancel();
        }

        RenderTargetTracker.markClean(entityTarget);
    }
}
