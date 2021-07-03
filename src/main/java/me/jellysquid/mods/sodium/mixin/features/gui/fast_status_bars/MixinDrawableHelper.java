package me.jellysquid.mods.sodium.mixin.features.gui.fast_status_bars;

import me.jellysquid.mods.sodium.client.render.gui.BatchedDrawableHelper;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DrawableHelper.class)
public class MixinDrawableHelper {
    @Inject(method = "drawTexturedQuad", at = @At("HEAD"), cancellable = true)
    private static void drawTexturedQuad(Matrix4f matrices, int x0, int x1, int y0, int y1, int z, float u0, float u1, float v0, float v1, CallbackInfo ci) {
        if (BatchedDrawableHelper.inTextureBatch) {
            BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
            bufferBuilder.vertex(matrices, (float) x0, (float) y1, (float) z).texture(u0, v1).next();
            bufferBuilder.vertex(matrices, (float) x1, (float) y1, (float) z).texture(u1, v1).next();
            bufferBuilder.vertex(matrices, (float) x1, (float) y0, (float) z).texture(u1, v0).next();
            bufferBuilder.vertex(matrices, (float) x0, (float) y0, (float) z).texture(u0, v0).next();
            ci.cancel();
        }
    }
}
