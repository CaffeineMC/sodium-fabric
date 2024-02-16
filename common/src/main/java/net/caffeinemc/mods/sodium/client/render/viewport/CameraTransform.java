package net.caffeinemc.mods.sodium.client.render.viewport;

import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;

public class CameraTransform {
    // We want to reduce the precision of the deltas to avoid seams along chunk/region boundaries. This is done by
    // ensuring the camera position would be the same if we did cameraPos + 0 - 0 as if we did cameraPos + 128 - 128.
    private static final float PRECISION_MODIFIER = RenderRegion.REGION_WIDTH * 16; // 16 blocks per section

    // The integer component of the translation vector
    public final int intX, intY, intZ;

    // The fractional component of the translation vector
    public final float fracX, fracY, fracZ;

    // The original transform.
    public final double x, y, z;

    public CameraTransform(double x, double y, double z) {
        this.intX = integral(x);
        this.intY = integral(y);
        this.intZ = integral(z);

        this.fracX = fractional(x);
        this.fracY = fractional(y);
        this.fracZ = fractional(z);

        this.x = x;
        this.y = y;
        this.z = z;
    }

    private static int integral(double value) {
        return (int) value;
    }

    private static float fractional(double value) {
        float fullPrecision = (float) (value - integral(value));
        float modifier = Math.copySign(PRECISION_MODIFIER, fullPrecision);

        return  (fullPrecision + modifier) - modifier;
    }
}
