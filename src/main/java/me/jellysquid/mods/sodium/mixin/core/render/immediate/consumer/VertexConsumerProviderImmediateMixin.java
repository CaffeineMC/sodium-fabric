package me.jellysquid.mods.sodium.mixin.core.render.immediate.consumer;

import com.mojang.blaze3d.systems.VertexSorter;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.render.vertex.buffer.ExtendedBufferBuilder;
import me.jellysquid.mods.sodium.client.render.vertex.buffer.SodiumBufferBuilder;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatRegistry;
import net.minecraft.client.render.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Map;
import java.util.Set;

@Mixin(VertexConsumerProvider.Immediate.class)
public class VertexConsumerProviderImmediateMixin {
    @Unique
    private final Map<VertexConsumer, SodiumBufferBuilder> bufferBuilderReplacements = new Reference2ReferenceOpenHashMap<>();

    @Redirect(method = "getBuffer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/BufferBuilder;begin(Lnet/minecraft/client/render/VertexFormat$DrawMode;Lnet/minecraft/client/render/VertexFormat;)V"))
    private void startReplacement(BufferBuilder instance, VertexFormat.DrawMode drawMode, VertexFormat format) {
        instance.begin(drawMode, format);
        if (VertexFormatRegistry.instance().get(format).isSimpleFormat()) {
            bufferBuilderReplacements.put(instance, new SodiumBufferBuilder((ExtendedBufferBuilder)instance));
        }
    }

    @Inject(method = "draw(Lnet/minecraft/client/render/RenderLayer;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/RenderLayer;draw(Lnet/minecraft/client/render/BufferBuilder;Lcom/mojang/blaze3d/systems/VertexSorter;)V"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void stopReplacement(RenderLayer layer, CallbackInfo ci, BufferBuilder buffer) {
        bufferBuilderReplacements.remove(buffer);
    }

    @Inject(method = "getBuffer", at = @At("RETURN"), cancellable = true)
    private void useFasterVertexConsumer(RenderLayer layer, CallbackInfoReturnable<VertexConsumer> cir) {
        SodiumBufferBuilder replacement = bufferBuilderReplacements.get(cir.getReturnValue());
        if (replacement != null) {
            cir.setReturnValue(replacement);
        }
    }
}
