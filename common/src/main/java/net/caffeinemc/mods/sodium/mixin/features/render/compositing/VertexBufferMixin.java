package net.caffeinemc.mods.sodium.mixin.features.render.compositing;

import com.mojang.blaze3d.vertex.VertexBuffer;
import net.caffeinemc.mods.sodium.client.render.util.RenderTargetTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VertexBuffer.class)
public class VertexBufferMixin {
    @Inject(method = "draw", at = @At("RETURN"))
    private void postDraw(CallbackInfo ci) {
        // When any geometry is drawn, mark the framebuffer that it was rasterized to
        RenderTargetTracker.notifyActiveWriteTargetModified();
    }
}
