package me.jellysquid.mods.sodium.mixin.features.buffer_builder.intrinsics;

import com.mojang.blaze3d.vertex.VertexConsumer;
import me.jellysquid.mods.sodium.interop.vanilla.vertex.VanillaVertexFormats;
import me.jellysquid.mods.sodium.render.vertex.VertexDrain;
import me.jellysquid.mods.sodium.render.vertex.VertexSink;
import me.jellysquid.mods.sodium.interop.vanilla.vertex.transformers.SpriteTexturedVertexTransformer;
import me.jellysquid.mods.sodium.render.vertex.type.VertexType;
import net.minecraft.client.renderer.SpriteCoordinateExpander;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SpriteCoordinateExpander.class)
public abstract class MixinSpriteTexturedVertexConsumer implements VertexDrain {
    @Shadow
    @Final
    private TextureAtlasSprite sprite;

    @Shadow
    @Final
    private VertexConsumer delegate;

    @SuppressWarnings("unchecked")
    @Override
    public <T extends VertexSink> T createSink(VertexType<T> type) {
        if (type == VanillaVertexFormats.QUADS) {
            return (T) new SpriteTexturedVertexTransformer.Quad(VertexDrain.of(this.delegate)
                    .createSink(VanillaVertexFormats.QUADS), this.sprite);
        } else if (type == VanillaVertexFormats.PARTICLES) {
            return (T) new SpriteTexturedVertexTransformer.Particle(VertexDrain.of(this.delegate)
                    .createSink(VanillaVertexFormats.PARTICLES), this.sprite);
        } else if (type == VanillaVertexFormats.GLYPHS) {
            return (T) new SpriteTexturedVertexTransformer.Glyph(VertexDrain.of(this.delegate)
                    .createSink(VanillaVertexFormats.GLYPHS), this.sprite);
        }

        return type.createFallbackWriter((VertexConsumer) this);
    }
}
