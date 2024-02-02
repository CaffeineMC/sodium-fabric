package me.jellysquid.mods.sodium.mixin.core.render.immediate.consumer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import me.jellysquid.mods.sodium.client.render.vertex.buffer.ExtendedBufferBuilder;
import me.jellysquid.mods.sodium.client.render.vertex.buffer.SodiumBufferBuilder;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiBufferSource.BufferSource.class)
public class VertexConsumerProviderImmediateMixin {
    @Inject(method = "getBuffer", at = @At("RETURN"), cancellable = true)
    private void useFasterVertexConsumer(RenderType layer, CallbackInfoReturnable<VertexConsumer> cir) {
        if (cir.getReturnValue() instanceof ExtendedBufferBuilder bufferBuilder) {
            SodiumBufferBuilder replacement = bufferBuilder.sodium$getDelegate();
            if (replacement != null) {
                cir.setReturnValue(replacement);
            }
        }
    }

    @ModifyVariable(method = "method_24213", at = @At(value = "LOAD", ordinal = 0))
    private VertexConsumer changeComparedVertexConsumer(VertexConsumer input) {
        if (input instanceof SodiumBufferBuilder replacement) {
            return replacement.getOriginalBufferBuilder();
        } else {
            return input;
        }
    }
}
