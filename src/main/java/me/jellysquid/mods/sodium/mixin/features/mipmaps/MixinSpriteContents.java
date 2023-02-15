package me.jellysquid.mods.sodium.mixin.features.mipmaps;

import me.jellysquid.mods.sodium.client.util.NativeImageHelper;
import me.jellysquid.mods.sodium.client.util.color.ColorSRGB;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.SpriteContents;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryUtil;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * This Mixin is ported from Iris at <a href="https://github.com/IrisShaders/Iris/blob/41095ac23ea0add664afd1b85c414d1f1ed94066/src/main/java/net/coderbot/iris/mixin/bettermipmaps/MixinTextureAtlasSprite.java">MixinTextureAtlasSprite</a>.
 */
@Mixin(SpriteContents.class)
public class MixinSpriteContents {
    @Mutable
    @Shadow
    @Final
    private NativeImage image;

    // While Fabric allows us to @Inject into the constructor here, that's just a specific detail of FabricMC's mixin
    // fork. Upstream Mixin doesn't allow arbitrary @Inject usage in constructor. However, we can use @ModifyVariable
    // just fine, in a way that hopefully doesn't conflict with other mods.
    //
    // By doing this, we can work with upstream Mixin as well, as is used on Forge. While we don't officially
    // support Forge, since this works well on Fabric too, it's fine to ensure that the diff between Fabric and Forge
    // can remain minimal. Being less dependent on specific details of Fabric is good, since it means we can be more
    // cross-platform.
    @Redirect(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/client/texture/SpriteContents;image:Lnet/minecraft/client/texture/NativeImage;", opcode = Opcodes.PUTFIELD))
    private void sodium$beforeGenerateMipLevels(SpriteContents instance, NativeImage nativeImage, Identifier identifier) {
        // We're injecting after the "info" field has been set, so this is safe even though we're in a constructor.
        sodium$fillInTransparentPixelColors(nativeImage);

        this.image = nativeImage;
    }

    /**
     * Fixes a common issue in image editing programs where fully transparent pixels are saved with fully black colors.
     *
     * This causes issues with mipmapped texture filtering, since the black color is used to calculate the final color
     * even though the alpha value is zero. While ideally it would be disregarded, we do not control that. Instead,
     * this code tries to calculate a decent average color to assign to these fully-transparent pixels so that their
     * black color does not leak over into sampling.
     */
    @Unique
    private static void sodium$fillInTransparentPixelColors(NativeImage nativeImage) {
        final long ppPixel = NativeImageHelper.getPointerRGBA(nativeImage);

        // Calculate an average color from all pixels that are not completely transparent.
        //
        // This average is weighted based on the (non-zero) alpha value of the pixel.
        float r = 0.0f;
        float g = 0.0f;
        float b = 0.0f;
        float totalAlpha = 0.0f;

        int pixelCount = nativeImage.getHeight() * nativeImage.getWidth();

        for (int pixelIndex = 0; pixelIndex < pixelCount; pixelIndex++) {
            long pPixel = ppPixel + (pixelIndex * 4);

            int color = MemoryUtil.memGetInt(pPixel);
            int alpha = NativeImage.getAlpha(color);

            if (alpha == 0) {
                // Ignore all fully-transparent pixels for the purposes of computing an average color.
                continue;
            }

            totalAlpha += alpha;

            // Make sure to convert to linear space so that we don't lose brightness.
            r += ColorSRGB.convertSRGBToLinear(NativeImage.getRed(color)) * alpha;
            g += ColorSRGB.convertSRGBToLinear(NativeImage.getGreen(color)) * alpha;
            b += ColorSRGB.convertSRGBToLinear(NativeImage.getBlue(color)) * alpha;
        }

        r /= totalAlpha;
        g /= totalAlpha;
        b /= totalAlpha;

        // If there weren't any pixels that were not fully transparent, bail out.
        if (totalAlpha == 0.0f) {
            return;
        }

        // Convert that color in linear space back to sRGB.
        // Use an alpha value of zero - this works since we only replace pixels with an alpha value of 0.
        int resultColor = ColorSRGB.convertLinearToSRGB(r, g, b, 0);

        for (int pixelIndex = 0; pixelIndex < pixelCount; pixelIndex++) {
            long pPixel = ppPixel + (pixelIndex * 4);

            int color = MemoryUtil.memGetInt(pPixel);
            int alpha = (color >> 24) & 255;

            // If this pixel has nonzero alpha, don't touch it.
            if (alpha > 0) {
                continue;
            }

            // Replace the color values of this pixel with the average colors.
            MemoryUtil.memPutInt(pPixel, resultColor);
        }
    }
}