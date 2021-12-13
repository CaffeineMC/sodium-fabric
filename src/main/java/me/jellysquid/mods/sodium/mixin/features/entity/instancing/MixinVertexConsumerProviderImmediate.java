package me.jellysquid.mods.sodium.mixin.features.entity.instancing;

import me.jellysquid.mods.sodium.SodiumClient;
import me.jellysquid.mods.sodium.SodiumRender;
import me.jellysquid.mods.sodium.interop.vanilla.layer.BufferBuilderExtended;
import me.jellysquid.mods.sodium.render.entity.renderer.InstancedEntityRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VertexConsumerProvider.Immediate.class)
public abstract class MixinVertexConsumerProviderImmediate {
    @Inject(method = "getBuffer", at = @At("RETURN"))
    private void attachRenderLayerToBuffer(RenderLayer renderLayer, CallbackInfoReturnable<VertexConsumer> cir) {
        VertexConsumer consumer = cir.getReturnValue();
        if (consumer instanceof BufferBuilderExtended bufferBuilderExtended
                && SodiumClient.options().performance.useModelInstancing
                && InstancedEntityRenderer.isSupported(SodiumRender.DEVICE)) {
            bufferBuilderExtended.setRenderLayer(renderLayer);
        }
    }
}
