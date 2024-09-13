package net.caffeinemc.mods.sodium.mixin.core.model;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(TextureAtlasSprite.class)
public class TextureAtlasSpriteMixin {
    @Overwrite
    public float uvShrinkRatio() {
        // Vanilla tries to apply a bias to texture coordinates to avoid texture bleeding (see FaceBakery#bakeQuad).
        // This is counterproductive with Sodium's terrain rendering, since the bias is applied in the shader instead.
        // Overwrite this method instead of adjusting its return value in FaceBakery as other mods may use it to
        // manually apply the bias.
        return 0.0f;
    }
}
