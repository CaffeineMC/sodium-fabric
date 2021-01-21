package me.jellysquid.mods.sodium.mixin.core.pipeline;

import me.jellysquid.mods.sodium.client.model.vertex.VertexDrain;
import me.jellysquid.mods.sodium.client.model.vertex.VertexSink;
import me.jellysquid.mods.sodium.client.model.vertex.type.VertexType;
import net.minecraft.client.render.VertexConsumer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(VertexConsumer.class)
public interface MixinVertexConsumer extends VertexDrain {
    @Override
    default <T extends VertexSink> T createSink(VertexType<T> factory) {
        return factory.createFallbackWriter((VertexConsumer) this);
    }
}
