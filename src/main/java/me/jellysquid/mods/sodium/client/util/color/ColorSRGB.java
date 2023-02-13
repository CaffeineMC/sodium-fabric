package me.jellysquid.mods.sodium.client.util.color;

import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import org.spongepowered.asm.mixin.Unique;

public class ColorSRGB {
    private static final double SRGB_CONVERSION_COMPONENT = 1.0 / 2.2;

    // Generate some color tables for gamma correction.
    private static final float[] SRGB_TO_LINEAR = new float[256];

    static {
        for (int i = 0; i < 256; i++) {
            SRGB_TO_LINEAR[i] = (float) Math.pow(i / 255.0, 2.2);
        }
    }

    public static float convertToSRGB(int linear) {
        return SRGB_TO_LINEAR[linear];
    }

    // Packs 3 color components and a linear alpha into sRGB from linear color space.
    @Unique
    public static int convertToPackedLinear(float r, float g, float b, int a) {
        int srgbR = (int) (Math.pow(r, SRGB_CONVERSION_COMPONENT) * 255.0);
        int srgbG = (int) (Math.pow(g, SRGB_CONVERSION_COMPONENT) * 255.0);
        int srgbB = (int) (Math.pow(b, SRGB_CONVERSION_COMPONENT) * 255.0);

        return ColorARGB.pack(srgbR, srgbG, srgbB, a);
    }
}
