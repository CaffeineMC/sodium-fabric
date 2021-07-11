package me.jellysquid.mods.sodium.mixin.features.item;

import me.jellysquid.mods.sodium.client.model.vertex.VanillaVertexTypes;
import me.jellysquid.mods.sodium.client.model.vertex.VertexDrain;
import me.jellysquid.mods.sodium.client.model.vertex.formats.generic.PositionTextureSink;
import me.jellysquid.mods.sodium.client.render.GuiRenderBatches;
import me.jellysquid.mods.sodium.client.render.ItemRendererExtended;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class MixinInGameHud extends DrawableHelper {
    @Shadow
    @Final
    private ItemRenderer itemRenderer;
    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method = "renderStatusBars", at = @At(value = "CONSTANT", args = "stringValue=armor"))
    private void preRenderStatusBars(MatrixStack matrices, CallbackInfo ci) {
        GuiRenderBatches.HUD.begin();
    }

    @Redirect(method = "renderStatusBars", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture(Lnet/minecraft/client/util/math/MatrixStack;IIIIII)V"))
    private void redirectStatusBarIconRender(InGameHud inGameHud, MatrixStack matrices, int x, int y, int u, int v, int width, int height) {
        this.drawBatchedTexture(matrices, x, y, u, v, width, height, GuiRenderBatches.HUD.icons);
    }

    @Redirect(method = "drawHeart", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture(Lnet/minecraft/client/util/math/MatrixStack;IIIIII)V"))
    private void redirectDrawHeart(InGameHud inGameHud, MatrixStack matrices, int x, int y, int u, int v, int width, int height) {
        this.drawBatchedTexture(matrices, x, y, u, v, width, height, GuiRenderBatches.HUD.icons);
    }

    @Inject(method = "renderStatusBars", at = @At(value = "TAIL"))
    private void postRenderStatusBars(MatrixStack matrices, CallbackInfo ci) {
        if (GuiRenderBatches.HUD.isBuilding()) {
            GuiRenderBatches.HUD.draw();
        }
    }

    @Inject(method = "renderHotbar", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderColor(FFFF)V", ordinal = 0))
    private void preRenderHotbar(float tickDelta, MatrixStack matrices, CallbackInfo ci) {
        GuiRenderBatches.HUD.begin();
    }

    @Redirect(method = "renderHotbar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;renderHotbarItem(IIFLnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;I)V"))
    private void redirectRenderHotbarItem(InGameHud inGameHud, int x, int y, float tickDelta, PlayerEntity player, ItemStack stack, int seed) {
        this.renderBatchedHotbarItem(ItemRendererExtended.cast(this.itemRenderer), this.client.textRenderer, x, y, tickDelta, player, stack, seed);
    }

    @Redirect(
            method = "renderHotbar",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture(Lnet/minecraft/client/util/math/MatrixStack;IIIIII)V"
            ),
            slice = @Slice(
                    from = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderTexture(ILnet/minecraft/util/Identifier;)V"),
                    to = @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions;attackIndicator:Lnet/minecraft/client/option/AttackIndicator;")
            )
    )
    private void redirectDrawWidgetTexture(InGameHud inGameHud, MatrixStack matrices, int x, int y, int u, int v, int width, int height) {
        this.drawBatchedTexture(matrices, x, y, u, v, width, height, GuiRenderBatches.HUD.widgets);
    }

    @Redirect(
            method = "renderHotbar",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture(Lnet/minecraft/client/util/math/MatrixStack;IIIIII)V"
            ),
            slice = @Slice(
                    from = @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions;attackIndicator:Lnet/minecraft/client/option/AttackIndicator;"),
                    to = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;disableBlend()V")
            )
    )
    private void redirectDrawIconTexture(InGameHud inGameHud, MatrixStack matrices, int x, int y, int u, int v, int width, int height) {
        this.drawBatchedTexture(matrices, x, y, u, v, width, height, GuiRenderBatches.HUD.icons);
    }

    @Inject(method = "renderHotbar", at = @At(value = "TAIL"))
    private void postRenderHotbar(float tickDelta, MatrixStack matrices, CallbackInfo ci) {
        if (GuiRenderBatches.HUD.isBuilding()) {
            GuiRenderBatches.HUD.draw();
        }
    }

    private void renderBatchedHotbarItem(ItemRendererExtended itemRenderer, TextRenderer textRenderer, int x, int y,
                                         float tickDelta, PlayerEntity player, ItemStack stack, int seed) {
        if (stack.isEmpty()) {
            return;
        }

        MatrixStack matrixStack = new MatrixStack();

        float cooldown = stack.getCooldown() - tickDelta;

        if (cooldown > 0.0F) {
            float f = 1.0F + (cooldown / 5.0F);

            matrixStack.translate(x + 8, y + 12, 0.0D);
            matrixStack.scale(1.0F / f, (f + 1.0F) / 2.0F, 1.0F);
            matrixStack.translate(-(x + 8), -(y + 12), 0.0D);
        }

        itemRenderer.renderItemModel(GuiRenderBatches.HUD, matrixStack, x, y, stack, player, seed);
        itemRenderer.renderItemLabel(GuiRenderBatches.HUD, textRenderer, matrixStack, x, y, stack, null);
        itemRenderer.renderItemOverlays(GuiRenderBatches.HUD, stack, x, y);
    }

    private void drawBatchedTexture(MatrixStack matrices, int x, int y, int u, int v, int width, int height, BufferBuilder bufferBuilder) {
        drawBatchedTexture(matrices, x, x + width, y, y + height, this.getZOffset(), width, height, (float)u, (float)v, bufferBuilder);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private static void drawBatchedTexture(MatrixStack matrices, float x0, float y0, float x1, float y1, float z, float regionWidth, float regionHeight, float u, float v, BufferBuilder bufferBuilder) {
        writeTexturedQuad(matrices.peek().getModel(), x0, y0, x1, y1, z, u / 256.0f, (u + regionWidth) / (float) 256, v / 256.0f, (v + regionHeight) / 256.0f, bufferBuilder);
    }

    private static void writeTexturedQuad(Matrix4f matrices, float x0, float x1, float y0, float y1, float depth, float u0, float u1, float v0, float v1, BufferBuilder bufferBuilder) {
        PositionTextureSink sink = VertexDrain.of(bufferBuilder)
                .createSink(VanillaVertexTypes.POSITION_TEXTURE);
        sink.ensureCapacity(4);
        sink.writeVertex(matrices, x0, y1, depth, u0, v1);
        sink.writeVertex(matrices, x1, y1, depth, u1, v1);
        sink.writeVertex(matrices, x1, y0, depth, u1, v0);
        sink.writeVertex(matrices, x0, y0, depth, u0, v0);
        sink.flush();
    }
}
