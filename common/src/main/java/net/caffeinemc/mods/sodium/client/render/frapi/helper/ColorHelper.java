/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.caffeinemc.mods.sodium.client.render.frapi.helper;

import java.nio.ByteOrder;

/**
 * Static routines of general utility for renderer implementations.
 * Renderers are not required to use these helpers, but they were
 * designed to be usable without the default renderer.
 */
public abstract class ColorHelper {
    private ColorHelper() { }

    private static final boolean BIG_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;

    /** Component-wise multiply. Components need to be in same order in both inputs! */
    public static int multiplyColor(int color1, int color2) {
        if (color1 == -1) {
            return color2;
        } else if (color2 == -1) {
            return color1;
        }

        final int alpha = ((color1 >>> 24) & 0xFF) * ((color2 >>> 24) & 0xFF) / 0xFF;
        final int red = ((color1 >>> 16) & 0xFF) * ((color2 >>> 16) & 0xFF) / 0xFF;
        final int green = ((color1 >>> 8) & 0xFF) * ((color2 >>> 8) & 0xFF) / 0xFF;
        final int blue = (color1 & 0xFF) * (color2 & 0xFF) / 0xFF;

        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    /** Multiplies three lowest components by shade. High byte (usually alpha) unchanged. */
    public static int multiplyRGB(int color, float shade) {
        final int alpha = ((color >>> 24) & 0xFF);
        final int red = (int) (((color >>> 16) & 0xFF) * shade);
        final int green = (int) (((color >>> 8) & 0xFF) * shade);
        final int blue = (int) ((color & 0xFF) * shade);

        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    /**
     * Component-wise max.
     */
    public static int maxBrightness(int b0, int b1) {
        if (b0 == 0) return b1;
        if (b1 == 0) return b0;

        return Math.max(b0 & 0xFFFF, b1 & 0xFFFF) | Math.max(b0 & 0xFFFF0000, b1 & 0xFFFF0000);
    }

    /*
    Renderer color format: ARGB (0xAARRGGBB)
    Vanilla color format (little endian): ABGR (0xAABBGGRR)
    Vanilla color format (big endian): RGBA (0xRRGGBBAA)

    Why does the vanilla color format change based on endianness?
    See VertexConsumer#quad. Quad data is loaded as integers into
    a native byte order buffer. Color is read directly from bytes
    12, 13, 14 of each vertex. A different byte order will yield
    different results.

    The renderer always uses ARGB because the API color methods
    always consume and return ARGB. Vanilla block and item colors
    also use ARGB.
     */

    /**
     * Converts from ARGB color to ABGR color if little endian or RGBA color if big endian.
     */
    public static int toVanillaColor(int color) {
        if (color == -1) {
            return -1;
        }

        if (BIG_ENDIAN) {
            // ARGB to RGBA
            return ((color & 0x00FFFFFF) << 8) | ((color & 0xFF000000) >>> 24);
        } else {
            // ARGB to ABGR
            return (color & 0xFF00FF00) | ((color & 0x00FF0000) >>> 16) | ((color & 0x000000FF) << 16);
        }
    }

    /**
     * Converts to ARGB color from ABGR color if little endian or RGBA color if big endian.
     */
    public static int fromVanillaColor(int color) {
        if (color == -1) {
            return -1;
        }

        if (BIG_ENDIAN) {
            // RGBA to ARGB
            return ((color & 0xFFFFFF00) >>> 8) | ((color & 0x000000FF) << 24);
        } else {
            // ABGR to ARGB
            return (color & 0xFF00FF00) | ((color & 0x00FF0000) >>> 16) | ((color & 0x000000FF) << 16);
        }
    }
}
