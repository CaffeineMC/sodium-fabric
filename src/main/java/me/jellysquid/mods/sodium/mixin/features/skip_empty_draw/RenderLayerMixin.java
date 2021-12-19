package me.jellysquid.mods.sodium.mixin.features.skip_empty_draw;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;

@Mixin(RenderLayer.class)
public abstract class RenderLayerMixin extends RenderPhase {
    public RenderLayerMixin(String name, Runnable beginAction, Runnable endAction) {
        super(name, beginAction, endAction);
    }

    /**
     * Setting the GL state takes time. If we can avoid it whenever possible, we should.
     * If we know we're drawing no vertices, we shouldn't set the GL state beforehand.
     */
    @Inject(method = "draw", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/RenderLayer;startDrawing()V", shift = At.Shift.BEFORE), cancellable = true)
    private void trySkipSetState(BufferBuilder buffer, int cameraX, int cameraY, int cameraZ, CallbackInfo ci) {
        Pair<BufferBuilder.DrawArrayParameters, ByteBuffer> pair = buffer.popData();
        BufferBuilder.DrawArrayParameters drawArrayParameters = pair.getFirst();
        int count = drawArrayParameters.getCount();
        if (count > 0) {
            if (!RenderSystem.isOnRenderThreadOrInit()) {
                // In the original method, drawing likely doesn't work when not called from the render thread because of the GL state.
                // On the other hand, this method fixes that. If something relies on this breaking, we can re-break it by moving
                // the startDrawing and endDrawing to outside the render call.
                RenderSystem.recordRenderCall(() -> {
                    this.startDrawing();
                    BufferRendererAccessor.drawInternal(pair.getSecond(), drawArrayParameters.getMode(), drawArrayParameters.getVertexFormat(), drawArrayParameters.getCount(), drawArrayParameters.getElementFormat(), drawArrayParameters.getVertexCount(), drawArrayParameters.isTextured());
                    this.endDrawing();
                });
            } else {
                this.startDrawing();
                BufferRendererAccessor.drawInternal(pair.getSecond(), drawArrayParameters.getMode(), drawArrayParameters.getVertexFormat(), drawArrayParameters.getCount(), drawArrayParameters.getElementFormat(), drawArrayParameters.getVertexCount(), drawArrayParameters.isTextured());
                this.endDrawing();
            }
        }
        ci.cancel();
    }
}
