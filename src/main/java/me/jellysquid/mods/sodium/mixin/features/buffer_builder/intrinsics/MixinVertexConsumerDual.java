package me.jellysquid.mods.sodium.mixin.features.buffer_builder.intrinsics;

import me.jellysquid.mods.sodium.client.model.vertex.VanillaVertexTypes;
import me.jellysquid.mods.sodium.client.model.vertex.VertexDrain;
import me.jellysquid.mods.sodium.client.model.vertex.VertexSink;
import me.jellysquid.mods.sodium.client.model.vertex.formats.ModelQuadVertexSink;
import me.jellysquid.mods.sodium.client.model.vertex.transformers.DualModelQuadVertexSink;
import me.jellysquid.mods.sodium.client.model.vertex.type.VertexType;
import net.minecraft.client.render.VertexConsumer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net/minecraft/client/render/VertexConsumers$Dual")
public abstract class MixinVertexConsumerDual implements VertexDrain, VertexConsumer {
    @Shadow
    @Final
    private VertexConsumer first;

    @Shadow
    @Final
    private VertexConsumer second;

    @SuppressWarnings("unchecked")
    @Override
    public <T extends VertexSink> T createSink(VertexType<T> factory) {
        if (factory == VanillaVertexTypes.QUADS) {
            T first = VertexDrain.of(this.first)
                    .createSink(factory);

            T second = VertexDrain.of(this.second)
                    .createSink(factory);

            return (T) new DualModelQuadVertexSink((ModelQuadVertexSink) first, (ModelQuadVertexSink) second);
        }

        return factory.createFallbackWriter(this);
    }
}
