package me.jellysquid.mods.sodium.client.util.color;

import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.Validate;

public class BoxBlur {

    public static void blur(ColorBuffer buffer, int radius) {
        if (buffer.isHomogenous()) {
            return;
        }

        var tmp = new ColorBuffer(buffer.width, buffer.height);
        blur(buffer, tmp, radius); // X-axis
        blur(tmp, buffer, radius); // Y-axis
    }

    private static void blur(ColorBuffer src, ColorBuffer dst, int radius) {
        Validate.isTrue(src.width == dst.width);
        Validate.isTrue(src.height == dst.height);

        int multiplier = getAveragingMultiplier((radius * 2) + 1);

        int width = src.width;
        int height = src.height;

        for (int y = 0; y < height; y++) {
            int srcRowOffset = src.getIndex(0, y);

            int red = src.getRed(srcRowOffset);
            int green = src.getGreen(srcRowOffset);
            int blue = src.getBlue(srcRowOffset);

            // Extend the window backwards by repeating the colors at the edge N times
            red += red * radius;
            green += green * radius;
            blue += blue * radius;

            // Extend the window forwards by sampling ahead N times
            for (int x = 1; x <= radius; x++) {
                red += src.getRed(srcRowOffset + x);
                green += src.getGreen(srcRowOffset + x);
                blue += src.getBlue(srcRowOffset + x);
            }

            for (int x = 0; x < width; x++) {
                // The x and y coordinates are transposed to flip the output image
                dst.setARGB(dst.getIndex(y, x), averageRGB(red, green, blue, multiplier));

                {
                    int prevX = Math.max(0, x - radius);

                    // Remove the color values that are behind the window
                    red -= src.getRed(srcRowOffset + prevX);
                    green -= src.getGreen(srcRowOffset + prevX);
                    blue -= src.getBlue(srcRowOffset + prevX);
                }

                {
                    int nextX = Math.min(width - 1, x + radius + 1);

                    // Add the color values that are ahead of the window
                    red += src.getRed(srcRowOffset + nextX);
                    green += src.getGreen(srcRowOffset + nextX);
                    blue += src.getBlue(srcRowOffset + nextX);
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
        return MathHelper.ceil((1L << 24) / (double) size);
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

    public static class ColorBuffer {
        private final int[] data;

        private final int width, height;

        public ColorBuffer(int width, int height) {
            this.data = new int[width * height];
            this.width = width;
            this.height = height;
        }

        public int getIndex(int x, int y) {
            return (y * this.width) + x;
        }

        public void setARGB(int index, int packed)
        {
            this.data[index] = packed;
        }

        public int getARGB(int index) {
            return this.data[index];
        }

        public int getARGB(int x, int z) {
            return this.data[this.getIndex(x, z)];
        }

        public int getRed(int index) {
            return ColorARGB.unpackRed(this.getARGB(index));
        }

        public int getGreen(int index) {
            return ColorARGB.unpackGreen(this.getARGB(index));
        }

        public int getBlue(int index) {
            return ColorARGB.unpackBlue(this.getARGB(index));
        }

        public boolean isHomogenous() {
            int[] data = this.data;
            int first = data[0];

            for (int i = 1; i < data.length; i++) {
                if (data[i] != first) {
                    return false;
                }
            }

            return true;
        }
    }
}
