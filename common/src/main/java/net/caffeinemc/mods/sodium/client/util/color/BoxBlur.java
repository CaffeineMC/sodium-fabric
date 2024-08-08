package net.caffeinemc.mods.sodium.client.util.color;

import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.minecraft.util.Mth;

public class BoxBlur {

    public static void blur(ColorBuffer buf, ColorBuffer tmp, int radius) {
        if (buf.width != tmp.width || buf.height != tmp.height) {
            throw new IllegalArgumentException("Color buffers must have same dimensions");
        }

        if (isHomogenous(buf.data)) {
            return;
        }

        blurImpl(buf.data, tmp.data, buf.width, buf.height, radius); // X-axis
        blurImpl(tmp.data, buf.data, buf.width, buf.height, radius); // Y-axis
    }

    private static void blurImpl(int[] src, int[] dst, int width, int height, int radius) {
        int multiplier = getAveragingMultiplier((radius * 2) + 1);

        for (int y = 0; y < height; y++) {
            int srcRowOffset = ColorBuffer.getIndex(0, y, width);

            int red, green, blue;

            {
                int color = src[srcRowOffset];
                red = ColorARGB.unpackRed(color);
                green = ColorARGB.unpackGreen(color);
                blue = ColorARGB.unpackBlue(color);
            }

            // Extend the window backwards by repeating the colors at the edge N times
            red += red * radius;
            green += green * radius;
            blue += blue * radius;

            // Extend the window forwards by sampling ahead N times
            for (int x = 1; x <= radius; x++) {
                var color = src[srcRowOffset + x];
                red += ColorARGB.unpackRed(color);
                green += ColorARGB.unpackGreen(color);
                blue += ColorARGB.unpackBlue(color);
            }

            for (int x = 0; x < width; x++) {
                // The x and y coordinates are transposed to flip the output image
                dst[ColorBuffer.getIndex(y, x, width)] = averageRGB(red, green, blue, multiplier);

                {
                    // Remove the color values that are behind the window
                    var color = src[srcRowOffset + Math.max(0, x - radius)];

                    red -= ColorARGB.unpackRed(color);
                    green -= ColorARGB.unpackGreen(color);
                    blue -= ColorARGB.unpackBlue(color);
                }

                {
                    // Add the color values that are ahead of the window
                    var color = src[srcRowOffset + Math.min(width - 1, x + radius + 1)];
                    red += ColorARGB.unpackRed(color);
                    green += ColorARGB.unpackGreen(color);
                    blue += ColorARGB.unpackBlue(color);
                }
            }
        }
    }

    /**
     * Pre-computes a multiplier that can be used to avoid costly division when averaging the color data in the
     * sliding window.
     * @param size The size of the rolling window
     * @author 2No2Name
     */
    private static int getAveragingMultiplier(int size) {
        return Mth.ceil((1L << 24) / (double) size);
    }

    /**
     * Calculates the average color within the sliding window using the pre-computed constant.
     * @param multiplier The pre-computed constant provided by {@link BoxBlur#getAveragingMultiplier(int)} for a window
     *                   of the given size
     * @author 2No2Name
     */
    public static int averageRGB(int red, int green, int blue, int multiplier) {
        int value = 0xFF << 24; // Alpha is constant (fully opaque)
        value |= ((blue * multiplier) >>> 24) << 0;
        value |= ((green * multiplier) >>> 24) << 8;
        value |= ((red * multiplier) >>> 24) << 16;

        return value;
    }

    private static boolean isHomogenous(int[] array) {
        int first = array[0];

        for (int i = 1; i < array.length; i++) {
            if (array[i] != first) {
                return false;
            }
        }

        return true;
    }

    public static class ColorBuffer {
        protected final int[] data;
        protected final int width, height;

        public ColorBuffer(int width, int height) {
            this.data = new int[width * height];
            this.width = width;
            this.height = height;
        }

        public void set(int x, int y, int color) {
            this.data[getIndex(x, y, this.width)] = color;
        }


        public int get(int x, int y) {
            return this.data[getIndex(x, y, this.width)];
        }

        public static int getIndex(int x, int y, int width) {
            return (y * width) + x;
        }
    }
}
