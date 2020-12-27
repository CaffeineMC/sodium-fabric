package me.jellysquid.mods.sodium.mixin.features.buffer_builder.intrinsics;

import me.jellysquid.mods.sodium.client.model.vertex.VertexDrain;
import me.jellysquid.mods.sodium.client.model.vertex.VertexSink;
import me.jellysquid.mods.sodium.client.model.vertex.VertexSinkFactory;
import me.jellysquid.mods.sodium.client.model.vertex.transformers.SpriteTexturedVertexTransformer;
import net.minecraft.client.render.SpriteTexturedVertexConsumer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.Sprite;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SpriteTexturedVertexConsumer.class)
public abstract class MixinSpriteTexturedVertexConsumer implements VertexDrain {
    @Shadow
    @Final
    private Sprite sprite;

    @Shadow
    @Final
    private VertexConsumer parent;

    @Override
    public <T extends VertexSink> T createSink(VertexSinkFactory<T> factory) {
        T sink = VertexDrain.of(this.parent)
                .createSink(factory);

        return factory.createTransformingSink(sink, new SpriteTexturedVertexTransformer(this.sprite));
    }
}
