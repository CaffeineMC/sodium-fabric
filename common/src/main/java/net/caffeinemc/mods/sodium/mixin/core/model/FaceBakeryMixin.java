package net.caffeinemc.mods.sodium.mixin.core.model;

import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FaceBakery.class)
public class FaceBakeryMixin {
    @Redirect(method = "bakeQuad", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;uvShrinkRatio()F"))
    private float alterUvShrinkRatio(TextureAtlasSprite sprite) {
        // Vanilla tries to apply a bias to texture coordinates to avoid texture bleeding. This is counterproductive
        // with Sodium's terrain rendering, since the bias is applied in the shader instead.
        return 0.0f;
    }
}
