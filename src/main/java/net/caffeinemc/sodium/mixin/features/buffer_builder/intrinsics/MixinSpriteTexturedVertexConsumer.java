package net.caffeinemc.sodium.mixin.features.buffer_builder.intrinsics;

import net.caffeinemc.sodium.interop.vanilla.vertex.VanillaVertexFormats;
import net.caffeinemc.sodium.render.vertex.VertexDrain;
import net.caffeinemc.sodium.render.vertex.VertexSink;
import net.caffeinemc.sodium.interop.vanilla.vertex.transformers.SpriteTexturedVertexTransformer;
import net.caffeinemc.sodium.render.vertex.type.VertexType;
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

    @SuppressWarnings("unchecked")
    @Override
    public <T extends VertexSink> T createSink(VertexType<T> type) {
        if (type == VanillaVertexFormats.QUADS) {
            return (T) new SpriteTexturedVertexTransformer.ModelQuad(VertexDrain.of(this.parent)
                    .createSink(VanillaVertexFormats.QUADS), this.sprite);
        } else if (type == VanillaVertexFormats.PARTICLES) {
            return (T) new SpriteTexturedVertexTransformer.Particle(VertexDrain.of(this.parent)
                    .createSink(VanillaVertexFormats.PARTICLES), this.sprite);
        } else if (type == VanillaVertexFormats.GLYPHS) {
            return (T) new SpriteTexturedVertexTransformer.Glyph(VertexDrain.of(this.parent)
                    .createSink(VanillaVertexFormats.GLYPHS), this.sprite);
        }

        return type.createFallbackWriter((VertexConsumer) this);
    }
}
