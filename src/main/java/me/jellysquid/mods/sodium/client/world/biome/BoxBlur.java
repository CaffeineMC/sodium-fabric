package me.jellysquid.mods.sodium.client.world.biome;

import me.jellysquid.mods.sodium.client.util.color.ColorARGB;
import org.apache.commons.lang3.Validate;

public class BoxBlur {
    /**
     * Performs a box blur with the specified radius on the 2D array of color values.
     *
     * @param data The array of color values to blur in-place
     * @param width The width of the array
     * @param height The height of the array
     * @param radius The radius to blur with
     */
    public static void blur(int[] data, int width, int height, int radius) {
        Validate.isTrue(data.length == (width * height), "data.length != (width * height)");

        // TODO: Re-use allocations between invocations
        var tmp = new int[width * height];
        blur(data, tmp, width, height, radius); // X-axis
        blur(tmp, data, width, height, radius); // Y-axis
    }

    private static void blur(int[] src, int[] dst, final int width, final int height, int radius) {
        final int windowSize = (radius * 2) + 1;
        final int edgeExtendAmount = radius + 1;

        int srcIndex = 0;

        // TODO: SIMD
        for (int y = 0; y < height; y++) {
            int dstIndex = y;
            int color = src[srcIndex];

            int alpha = ColorARGB.unpackAlpha(color);
            int red = ColorARGB.unpackRed(color);
            int green = ColorARGB.unpackGreen(color);
            int blue = ColorARGB.unpackBlue(color);

            // Extend the window backwards by repeating the colors at the edge N times
            alpha *= edgeExtendAmount;
            red *= edgeExtendAmount;
            green *= edgeExtendAmount;
            blue *= edgeExtendAmount;

            // Extend the window forwards by sampling ahead N times
            for (int i = 1; i <= radius; i++) {
                int neighborColor = src[srcIndex + i];

                alpha += ColorARGB.unpackAlpha(neighborColor);
                red += ColorARGB.unpackRed(neighborColor);
                green += ColorARGB.unpackGreen(neighborColor);
                blue += ColorARGB.unpackBlue(neighborColor);
            }

            for (int x = 0; x < width; x++) {
                // TODO: Avoid division
                dst[dstIndex] = ColorARGB.pack(red / windowSize, green / windowSize,
                        blue / windowSize, alpha / windowSize);

                int previousPixelIndex = Math.max(x - radius, 0);
                int previousPixel = src[srcIndex + previousPixelIndex];

                // Remove the color values that are behind the window
                alpha -= ColorARGB.unpackAlpha(previousPixel);
                red -= ColorARGB.unpackRed(previousPixel);
                green -= ColorARGB.unpackGreen(previousPixel);
                blue -= ColorARGB.unpackBlue(previousPixel);

                int nextPixelIndex = Math.min(x + edgeExtendAmount, width - 1);
                int nextPixel = src[srcIndex + nextPixelIndex];

                // Add the color values that are ahead of the window
                alpha += ColorARGB.unpackAlpha(nextPixel);
                red += ColorARGB.unpackRed(nextPixel);
                green += ColorARGB.unpackGreen(nextPixel);
                blue += ColorARGB.unpackBlue(nextPixel);

                // Move the window forward
                dstIndex += width;
            }

            srcIndex += width;
        }
    }
}
