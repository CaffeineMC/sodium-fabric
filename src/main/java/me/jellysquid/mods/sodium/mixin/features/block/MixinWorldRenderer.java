package me.jellysquid.mods.sodium.mixin.features.block;

import me.jellysquid.mods.sodium.client.render.pipeline.context.ChunkRenderCacheShared;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class MixinWorldRenderer {
    /**
     * Reset any global cached state before rendering a frame. This will hopefully ensure that any world state that has
     * changed is reflected in vanilla-style rendering.
     */
    @Inject(method = "render", at = @At("HEAD"))
    private void reset(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera,
                       GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f,
                       CallbackInfo ci) {
        ChunkRenderCacheShared.resetCaches();
    }
}
