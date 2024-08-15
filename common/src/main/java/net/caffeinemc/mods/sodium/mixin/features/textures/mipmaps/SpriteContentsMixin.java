/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Original source: https://github.com/IrisShaders/Iris/blob/41095ac23ea0add664afd1b85c414d1f1ed94066/src/main/java/net/coderbot/iris/mixin/bettermipmaps/MixinTextureAtlasSprite.java
 */
package net.caffeinemc.mods.sodium.mixin.features.textures.mipmaps;

import com.mojang.blaze3d.platform.NativeImage;
import net.caffeinemc.mods.sodium.client.util.NativeImageHelper;
import net.caffeinemc.mods.sodium.client.util.color.ColorSRGB;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import org.lwjgl.system.MemoryUtil;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SpriteContents.class)
public class SpriteContentsMixin {
    @Mutable
    @Shadow
    @Final
    private NativeImage originalImage;

    // While Fabric allows us to @Inject into the constructor here, that's just a specific detail of FabricMC's mixin
    // fork. Upstream Mixin doesn't allow arbitrary @Inject usage in constructor. However, we can use @ModifyVariable
    // just fine, in a way that hopefully doesn't conflict with other mods.
    //
    // By doing this, we can work with upstream Mixin as well, as is used on Forge. While we don't officially
    // support Forge, since this works well on Fabric too, it's fine to ensure that the diff between Fabric and Forge
    // can remain minimal. Being less dependent on specific details of Fabric is good, since it means we can be more
    // cross-platform.
    @Redirect(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/texture/SpriteContents;originalImage:Lcom/mojang/blaze3d/platform/NativeImage;", opcode = Opcodes.PUTFIELD))
    private void sodium$beforeGenerateMipLevels(SpriteContents instance, NativeImage nativeImage, ResourceLocation identifier) {
        // We're injecting after the "info" field has been set, so this is safe even though we're in a constructor.
        sodium$fillInTransparentPixelColors(nativeImage);

        this.originalImage = nativeImage;
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
        final int pixelCount = nativeImage.getHeight() * nativeImage.getWidth();

        // Calculate an average color from all pixels that are not completely transparent.
        // This average is weighted based on the (non-zero) alpha value of the pixel.
        float r = 0.0f;
        float g = 0.0f;
        float b = 0.0f;

        float totalWeight = 0.0f;

        for (int pixelIndex = 0; pixelIndex < pixelCount; pixelIndex++) {
            long pPixel = ppPixel + (pixelIndex * 4);

            int color = MemoryUtil.memGetInt(pPixel);
            int alpha = FastColor.ABGR32.alpha(color);

            // Ignore all fully-transparent pixels for the purposes of computing an average color.
            if (alpha != 0) {
                float weight = (float) alpha;

                // Make sure to convert to linear space so that we don't lose brightness.
                r += ColorSRGB.srgbToLinear(FastColor.ABGR32.red(color)) * weight;
                g += ColorSRGB.srgbToLinear(FastColor.ABGR32.green(color)) * weight;
                b += ColorSRGB.srgbToLinear(FastColor.ABGR32.blue(color)) * weight;

                totalWeight += weight;
            }
        }

        // Bail if none of the pixels are semi-transparent.
        if (totalWeight == 0.0f) {
            return;
        }

        r /= totalWeight;
        g /= totalWeight;
        b /= totalWeight;

        // Convert that color in linear space back to sRGB.
        // Use an alpha value of zero - this works since we only replace pixels with an alpha value of 0.
        int averageColor = ColorSRGB.linearToSrgb(r, g, b, 0);

        for (int pixelIndex = 0; pixelIndex < pixelCount; pixelIndex++) {
            long pPixel = ppPixel + (pixelIndex * 4);

            int color = MemoryUtil.memGetInt(pPixel);
            int alpha = FastColor.ABGR32.alpha(color);

            // Replace the color values of pixels which are fully transparent, since they have no color data.
            if (alpha == 0) {
                MemoryUtil.memPutInt(pPixel, averageColor);
            }
        }
    }
}