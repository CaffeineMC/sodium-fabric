package me.jellysquid.mods.sodium.mixin.features.block;

import me.jellysquid.mods.sodium.client.render.BatchingVertexConsumer;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.SignBlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SignBlockEntityRenderer.class)
public class MixinSignBlockEntityRenderer {
    @Shadow
    @Final
    private TextRenderer textRenderer;

    private final BatchingVertexConsumer batchingVertexConsumer = new BatchingVertexConsumer();

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;pop()V", ordinal = 1, shift = At.Shift.BEFORE))
    private void postRender(SignBlockEntity signBlockEntity, float f, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, int j, CallbackInfo ci) {
        this.batchingVertexConsumer.draw();
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/font/TextRenderer;draw(Lnet/minecraft/text/OrderedText;FFIZLnet/minecraft/util/math/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;ZII)I"))
    private int redirectDraw(TextRenderer textRenderer, OrderedText text, float x, float y, int color, boolean shadow, Matrix4f matrix, VertexConsumerProvider vertexConsumers, boolean seeThrough, int backgroundColor, int light) {
        return this.textRenderer.draw(text, x, y, color, shadow, matrix, this.batchingVertexConsumer, seeThrough, backgroundColor, light);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/font/TextRenderer;drawWithOutline(Lnet/minecraft/text/OrderedText;FFIILnet/minecraft/util/math/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"))
    private void redirectDrawWithOutline(TextRenderer textRenderer, OrderedText text, float x, float y, int color, int outlineColor, Matrix4f matrix, VertexConsumerProvider vertexConsumers, int light) {
        this.textRenderer.drawWithOutline(text, x, y, color, outlineColor, matrix, this.batchingVertexConsumer, light);
    }
}
