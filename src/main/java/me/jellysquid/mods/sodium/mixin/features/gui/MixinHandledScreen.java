package me.jellysquid.mods.sodium.mixin.features.gui;

import com.mojang.datafixers.util.Pair;
import me.jellysquid.mods.sodium.client.model.vertex.VanillaVertexTypes;
import me.jellysquid.mods.sodium.client.model.vertex.VertexDrain;
import me.jellysquid.mods.sodium.client.model.vertex.formats.generic.PositionColorSink;
import me.jellysquid.mods.sodium.client.render.GuiRenderBatches;
import me.jellysquid.mods.sodium.client.render.ItemRendererExtended;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(HandledScreen.class)
public abstract class MixinHandledScreen<T extends ScreenHandler> extends Screen {
    @Shadow
    protected int backgroundWidth;

    protected MixinHandledScreen(Text title) {
        super(title);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void preRender(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        GuiRenderBatches.CONTAINER.begin();
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;drawForeground(Lnet/minecraft/client/util/math/MatrixStack;II)V", shift = At.Shift.BEFORE))
    private void postRender(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        GuiRenderBatches.CONTAINER.draw();
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;drawSlotHighlight(Lnet/minecraft/client/util/math/MatrixStack;III)V"))
    private void redirectDrawSlotHighlight(MatrixStack matrices, int x, int y, int z) {
        fillGradient(matrices.peek().getModel(), GuiRenderBatches.CONTAINER.getSlotOverlayBuffer(), x, y, x + 16, y + 16, z, 0x80ffffff, 0x80ffffff);
    }

    /**
     * @author JellySquid
     * @reason Avoid draw calls
     */
    @Inject(method = "drawSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;setZOffset(I)V", ordinal = 0, shift = At.Shift.BEFORE), cancellable = true, locals = LocalCapture.CAPTURE_FAILSOFT)
    private void replaceDrawSlotRender(MatrixStack matrices, Slot slot, CallbackInfo ci, int x, int y, ItemStack stack, boolean dragging, boolean highlighted, ItemStack z0, String label) {
        ci.cancel();
        this.itemRenderer.zOffset = 100.0F;

        if (stack.isEmpty() && slot.isEnabled()) {
            Pair<Identifier, Identifier> pair = slot.getBackgroundSprite();

            if (pair != null) {
                Sprite sprite = this.client.getSpriteAtlas(pair.getFirst()).apply(pair.getSecond());

                drawSprite(GuiRenderBatches.CONTAINER.getSpriteBufferBuilder(sprite.getAtlas().getId()),
                        matrices, x, y, this.getZOffset(), 16, 16, sprite);
                dragging = true;
            }
        }

        if (!dragging) {
            if (highlighted) {
                drawSlotOverlay(matrices.peek().getModel(), x, y, x + 16, y + 16, 0x80ffffff);
            }

            ItemRendererExtended itemRenderer = ItemRendererExtended.cast(this.itemRenderer);
            itemRenderer.renderItemModel(GuiRenderBatches.CONTAINER, matrices, x, y, stack, this.client.player, slot.x + slot.y * this.backgroundWidth);
            itemRenderer.renderItemLabel(GuiRenderBatches.CONTAINER, this.textRenderer, matrices, x, y, stack, label);
            itemRenderer.renderItemOverlays(GuiRenderBatches.CONTAINER, stack, x, y);
        }

        this.itemRenderer.zOffset = 0.0F;
        this.setZOffset(0);
    }

    private static void drawSlotOverlay(Matrix4f matrix, int x1, int y1, int x2, int y2, int color) {
        PositionColorSink sink = VertexDrain.of(GuiRenderBatches.CONTAINER.getSlotOverlayBuffer())
                .createSink(VanillaVertexTypes.POSITION_COLOR);
        sink.ensureCapacity(4);
        sink.writeQuad(matrix, x1, y2, 0.0f, color);
        sink.writeQuad(matrix, x2, y2, 0.0F, color);
        sink.writeQuad(matrix, x2, y1, 0.0F, color);
        sink.writeQuad(matrix, x1, y1, 0.0F, color);
        sink.flush();
    }

    private static void drawSprite(BufferBuilder bufferBuilder, MatrixStack matrices, int x, int y, int z, int width, int height, Sprite sprite) {
        drawTexturedQuad(bufferBuilder, matrices.peek().getModel(), x, x + width, y, y + height, z, sprite.getMinU(), sprite.getMaxU(), sprite.getMinV(), sprite.getMaxV());
    }

    private static void drawTexturedQuad(BufferBuilder bufferBuilder, Matrix4f matrices, int x0, int x1, int y0, int y1, int z, float u0, float u1, float v0, float v1) {
        bufferBuilder.vertex(matrices, (float)x0, (float)y1, (float)z).texture(u0, v1).next();
        bufferBuilder.vertex(matrices, (float)x1, (float)y1, (float)z).texture(u1, v1).next();
        bufferBuilder.vertex(matrices, (float)x1, (float)y0, (float)z).texture(u1, v0).next();
        bufferBuilder.vertex(matrices, (float)x0, (float)y0, (float)z).texture(u0, v0).next();
    }
}
