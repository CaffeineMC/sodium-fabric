package me.jellysquid.mods.sodium.mixin.features.gui.fast_status_bars;

import com.mojang.blaze3d.systems.RenderSystem;
import me.jellysquid.mods.sodium.client.render.gui.BatchedDrawableHelper;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class MixinInGameHud extends DrawableHelper {
    @Shadow
    protected abstract PlayerEntity getCameraPlayer();

    // It's possible for status bar rendering to be skipped
    private boolean isRenderingStatusBars;

    @Inject(method = "renderStatusBars", at = @At("HEAD"))
    private void preRenderStatusBars(MatrixStack matrices, CallbackInfo ci) {
        if (this.getCameraPlayer() != null) {
            BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);

            BatchedDrawableHelper.inTextureBatch = true;
            this.isRenderingStatusBars = true;
        } else {
            this.isRenderingStatusBars = false;
        }
    }

    @Inject(method = "renderStatusBars", at = @At("RETURN"))
    private void renderStatusBars(MatrixStack matrices, CallbackInfo ci) {
        if (this.isRenderingStatusBars) {
            BatchedDrawableHelper.inTextureBatch = false;

            BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
            bufferBuilder.end();
            BufferRenderer.draw(bufferBuilder);
        }
    }
}
