package me.jellysquid.mods.sodium.mixin.features.gui;

import me.jellysquid.mods.sodium.client.model.vertex.VanillaVertexTypes;
import me.jellysquid.mods.sodium.client.model.vertex.VertexDrain;
import me.jellysquid.mods.sodium.client.model.vertex.formats.generic.PositionColorSink;
import me.jellysquid.mods.sodium.client.render.GuiRenderBatches;
import me.jellysquid.mods.sodium.client.render.ItemRendererExtended;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;pop()V", shift = At.Shift.BEFORE))
    private void postRender(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        GuiRenderBatches.CONTAINER.draw();
    }

    /**
     * @author JellySquid
     * @reason Avoid draw calls
     */
    @Inject(method = "drawSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;setZOffset(I)V", ordinal = 0, shift = At.Shift.BEFORE), cancellable = true, locals = LocalCapture.CAPTURE_FAILSOFT)
    private void drawSlot(MatrixStack matrices, Slot slot, CallbackInfo ci, int x, int y, ItemStack stack, boolean dragging, boolean highlighted, ItemStack z0, String label) {
        ci.cancel();

//        if (itemStack.isEmpty() && slot.isEnabled()) {
//            Pair<Identifier, Identifier> pair = slot.getBackgroundSprite();
//
//            if (pair != null) {
//                Sprite sprite = this.client.getSpriteAtlas(pair.getFirst()).apply(pair.getSecond());
//                RenderSystem.setShaderTexture(0, sprite.getAtlas().getId());
//                drawSprite(matrices, i, j, this.getZOffset(), 16, 16, sprite);
//                bl2 = true;
//            }
//        }

        if (!dragging) {
            if (highlighted) {
                fill0(matrices.peek().getModel(), x, y, x + 16, y + 16, 0x80ffffff);
            }

            ItemRendererExtended itemRenderer = ItemRendererExtended.cast(this.itemRenderer);
            itemRenderer.renderItemModel(GuiRenderBatches.CONTAINER, matrices, x, y, stack, this.client.player, slot.x + slot.y * this.backgroundWidth);
            itemRenderer.renderItemLabel(GuiRenderBatches.CONTAINER, this.textRenderer, matrices, x, y, stack, label);
            itemRenderer.renderItemOverlays(GuiRenderBatches.CONTAINER, stack, x, y);
        }

        this.itemRenderer.zOffset = 0.0F;
        this.setZOffset(0);
    }

    private static void fill0(Matrix4f matrix, int x1, int y1, int x2, int y2, int color) {
        PositionColorSink sink = VertexDrain.of(GuiRenderBatches.CONTAINER.getItemOverlayBuffer())
                .createSink(VanillaVertexTypes.POSITION_COLOR);
        sink.ensureCapacity(4);
        sink.writeQuad(matrix, x1, y2, 0.0f, color);
        sink.writeQuad(matrix, x2, y2, 0.0F, color);
        sink.writeQuad(matrix, x2, y1, 0.0F, color);
        sink.writeQuad(matrix, x1, y1, 0.0F, color);
        sink.flush();
    }
}
