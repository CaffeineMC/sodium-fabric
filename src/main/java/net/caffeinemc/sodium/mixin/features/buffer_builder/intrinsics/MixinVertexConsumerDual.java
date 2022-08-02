package net.caffeinemc.sodium.mixin.features.buffer_builder.intrinsics;

import net.caffeinemc.sodium.interop.vanilla.vertex.VanillaVertexFormats;
import net.caffeinemc.sodium.interop.vanilla.vertex.formats.ModelQuadVertexSink;
import net.caffeinemc.sodium.interop.vanilla.vertex.transformers.DualModelQuadVertexSink;
import net.caffeinemc.sodium.render.vertex.VertexDrain;
import net.caffeinemc.sodium.render.vertex.VertexSink;
import net.caffeinemc.sodium.render.vertex.type.VertexType;
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
        if (factory == VanillaVertexFormats.QUADS) {
            T first = VertexDrain.of(this.first)
                    .createSink(factory);

            T second = VertexDrain.of(this.second)
                    .createSink(factory);

            return (T) new DualModelQuadVertexSink((ModelQuadVertexSink) first, (ModelQuadVertexSink) second);
        }

        return factory.createFallbackWriter(this);
    }
}