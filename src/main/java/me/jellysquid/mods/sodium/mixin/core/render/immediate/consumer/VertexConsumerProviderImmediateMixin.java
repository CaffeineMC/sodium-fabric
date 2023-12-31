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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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

    @Redirect(method = "draw(Lnet/minecraft/client/render/RenderLayer;)V", at = @At(value = "INVOKE", target = "Ljava/util/Set;remove(Ljava/lang/Object;)Z"))
    private boolean stopReplacement(Set<BufferBuilder> instance, Object bufferbuilder) {
        bufferBuilderReplacements.remove(bufferbuilder);
        return instance.remove(bufferbuilder);
    }

    @Inject(method = "getBuffer", at = @At("RETURN"), cancellable = true)
    private void useFasterVertexConsumer(RenderLayer layer, CallbackInfoReturnable<VertexConsumer> cir) {
        SodiumBufferBuilder replacement = bufferBuilderReplacements.get(cir.getReturnValue());
        if (replacement != null) {
            cir.setReturnValue(replacement);
        }
    }
}
