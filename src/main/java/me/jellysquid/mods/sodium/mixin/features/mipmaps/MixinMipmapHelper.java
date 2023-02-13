package me.jellysquid.mods.sodium.mixin.features.mipmaps;

import net.minecraft.client.texture.MipmapHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

/**
 * Implements a significantly enhanced mipmap downsampling filter.
 *
 * <p>This algorithm combines ideas from vanilla Minecraft -- using linear color spaces instead of sRGB for blending) --
 * with ideas from OptiFine -- using the alpha values for weighting in downsampling -- to produce a novel downsampling
 * algorithm for mipmapping that produces minimal visual artifacts.</p>
 *
 * <p>This implementation fixes a number of issues with other implementations:</p>
 *
 * <li>
 *     <ul>OptiFine blends in sRGB space, resulting in brightness losses.</ul>
 *     <ul>Vanilla applies gamma correction to alpha values, which has weird results when alpha values aren't the same.</ul>
 *     <ul>Vanilla computes a simple average of the 4 pixels, disregarding the relative alpha values of pixels. In
 *         cutout textures, this results in a lot of pixels with high alpha values and dark colors, causing visual
 *         artifacts.</ul>
 * </li>
 *
 * This Mixin is ported from Iris at <a href="https://github.com/IrisShaders/Iris/blob/41095ac23ea0add664afd1b85c414d1f1ed94066/src/main/java/net/coderbot/iris/mixin/bettermipmaps/MixinMipmapGenerator.java">MixinMipmapGenerator</a>.
 */
@Mixin(MipmapHelper.class)
public class MixinMipmapHelper {
    // Generate some color tables for gamma correction.
    private static final float[] SRGB_TO_LINEAR = new float[256];

    static {
        for (int i = 0; i < 256; i++) {
            SRGB_TO_LINEAR[i] = (float) Math.pow(i / 255.0, 2.2);
        }
    }

    /**
     * @author coderbot
     * @reason replace the vanilla blending function with our improved function
     */
    @Overwrite
    private static int blend(int one, int two, int three, int four, boolean checkAlpha) {
        // First blend horizontally, then blend vertically.
        //
        // This works well for the case where our change is the most impactful (grass side overlays)
        return weightedAverageColor(weightedAverageColor(one, two), weightedAverageColor(three, four));
    }

    @Unique
    private static int unpackAlpha(int color) {
        return (color >>> 24) & 255;
    }

    @Unique
    private static int weightedAverageColor(int one, int two) {
        int alphaOne = unpackAlpha(one);
        int alphaTwo = unpackAlpha(two);

        // In the case where the alpha values of the same, we can get by with an unweighted average.
        if (alphaOne == alphaTwo) {
            return averageRgb(one, two, alphaOne);
        }

        // If one of our pixels is fully transparent, ignore it.
        // We just take the value of the other pixel as-is. To compensate for not changing the color value, we
        // divide the alpha value by 4 instead of 2.
        if (alphaOne == 0) {
            return (two & 0x00FFFFFF) | ((alphaTwo / 4) << 24);
        }

        if (alphaTwo == 0) {
            return (one & 0x00FFFFFF) | ((alphaOne / 4) << 24);
        }

        // Use the alpha values to compute relative weights of each color.
        float scale = 1.0f / (alphaOne + alphaTwo);
        float relativeWeightOne = alphaOne * scale;
        float relativeWeightTwo = alphaTwo * scale;

        // Convert the color components into linear space, then multiply the corresponding weight.
        float oneR = unpackLinearComponent(one, 0) * relativeWeightOne;
        float oneG = unpackLinearComponent(one, 8) * relativeWeightOne;
        float oneB = unpackLinearComponent(one, 16) * relativeWeightOne;
        float twoR = unpackLinearComponent(two, 0) * relativeWeightTwo;
        float twoG = unpackLinearComponent(two, 8) * relativeWeightTwo;
        float twoB = unpackLinearComponent(two, 16) * relativeWeightTwo;

        // Combine the color components of each color
        float linearR = oneR + twoR;
        float linearG = oneG + twoG;
        float linearB = oneB + twoB;

        // Take the average alpha of both pixels
        int averageAlpha = (alphaOne + alphaTwo) / 2;

        // Convert to sRGB and pack the colors back into an integer.
        return packLinearToSrgb(linearR, linearG, linearB, averageAlpha);
    }

    @Unique
    private static float unpackLinearComponent(int color, int shift) {
        return SRGB_TO_LINEAR[(color >> shift) & 255];
    }

    @Unique
    private static int packLinearToSrgb(float r, float g, float b, int a) {
        int srgbR = (int) (Math.pow(r, 1.0 / 2.2) * 255.0);
        int srgbG = (int) (Math.pow(g, 1.0 / 2.2) * 255.0);
        int srgbB = (int) (Math.pow(b, 1.0 / 2.2) * 255.0);

        return (a << 24) | (srgbB << 16) | (srgbG << 8) | srgbR;
    }

    // Computes a non-weighted average of the two sRGB colors in linear space, avoiding brightness losses.
    @Unique
    private static int averageRgb(int a, int b, int alpha) {
        float ar = unpackLinearComponent(a, 0);
        float ag = unpackLinearComponent(a, 8);
        float ab = unpackLinearComponent(a, 16);
        float br = unpackLinearComponent(b, 0);
        float bg = unpackLinearComponent(b, 8);
        float bb = unpackLinearComponent(b, 16);

        return packLinearToSrgb((ar + br) / 2.0f, (ag + bg) / 2.0f, (ab + bb) / 2.0f, alpha);
    }
}
