/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Original source: https://github.com/IrisShaders/Iris/blob/41095ac23ea0add664afd1b85c414d1f1ed94066/src/main/java/net/coderbot/iris/mixin/bettermipmaps/MixinTextureAtlasSprite.java
 */
package net.caffeinemc.mods.sodium.mixin.features.textures.mipmaps;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.NativeImage;
import it.unimi.dsi.fastutil.longs.LongHeapPriorityQueue;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.caffeinemc.mods.sodium.client.util.NativeImageHelper;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.lwjgl.system.MemoryUtil;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

/**
 * This mixin fills in transparent pixels with the color of the closest non-transparent pixel to improve mipmapping. Often transparent pixels are black, which ends up as a too dark color in the mipmaps.
 *
 * @author douira
 */
@Mixin(SpriteContents.class)
public class SpriteContentsMixin {
    @Mutable
    @Shadow
    @Final
    private NativeImage originalImage;

    @WrapOperation(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/texture/SpriteContents;originalImage:Lcom/mojang/blaze3d/platform/NativeImage;", opcode = Opcodes.PUTFIELD))
    private void sodium$beforeGenerateMipLevels(SpriteContents instance, NativeImage nativeImage, Operation<Void> original) {
        dilateColorsToTransparentPixels(nativeImage);

        original.call(instance, nativeImage);
    }

    @Unique
    private static long encodeEntry(float distance, int index) {
        // assumes there's no more than 2^32 pixels
        return ((long) Float.floatToRawIntBits(distance) << 32) | index;
    }

    @Unique
    private static float decodeEntryDistance(long entry) {
        return Float.intBitsToFloat((int) (entry >> 32));
    }

    @Unique
    private static int decodeEntryIndex(long entry) {
        return (int) entry;
    }

    @Unique
    private static long encodeOrigin(float distance, int x, int y) {
        // assumes coordinates don't exceed 2^16
        return ((long) Float.floatToRawIntBits(distance) << 32) | ((long) y << 16) | x;
    }

    @Unique
    private static int decodeOriginX(long origin) {
        return (int) origin & 0xFFFF;
    }

    @Unique
    private static int decodeOriginY(long origin) {
        return ((int) origin >> 16) & 0xFFFF;
    }

    @Unique
    private static float decodeOriginDistance(long entry) {
        return Float.intBitsToFloat((int) (entry >> 32));
    }

    @Unique
    private final static long OPAQUE = -1L;
    @Unique
    private final static long UNVISITED = -2L;

    /**
     * The propagation algorithm uses a distance-keyed priority queue to generate a voronoi-like result. There's the caveat that it won't propagate thinner than one pixel wide areas if they collide with other areas of a different color.
     */
    @Unique
    private static void dilateColorsToTransparentPixels(NativeImage nativeImage) {
        final long ppPixel = NativeImageHelper.getPointerRGBA(nativeImage);
        final int pixelCount = nativeImage.getHeight() * nativeImage.getWidth();
        var width = nativeImage.getWidth();
        var height = nativeImage.getHeight();

        // the non-transparent pixel so far closest to each pixel, encoded as 16-bit x and y coordinates
        // negative values have special meanings
        var origins = new long[pixelCount];

        // approximates the required capacity
        int nodeCount = 0;

        for (int pixelIndex = 0; pixelIndex < pixelCount; pixelIndex++) {
            long pPixel = ppPixel + (pixelIndex * 4L);
            int color = MemoryUtil.memGetInt(pPixel);
            int alpha = ColorABGR.unpackAlpha(color);

            if (alpha != 0) {
                origins[pixelIndex] = OPAQUE;
            } else {
                origins[pixelIndex] = UNVISITED;
                nodeCount++;
            }
        }

        if (nodeCount == pixelCount || nodeCount == 0) {
            return;
        }

        // the queue encodes the distance and the pixel index
        var queue = new LongHeapPriorityQueue(nodeCount);

        for (int pixelIndex = 0, x = 0, y = 0; pixelIndex < pixelCount; pixelIndex++) {
            if (origins[pixelIndex] == OPAQUE) {
                // check direct neighbors for being transparent
                boolean shouldEnqueue = false;
                if (x > 0) {
                    shouldEnqueue |= checkInitNeighbor(origins, pixelIndex - 1);
                }
                if (x < width - 1) {
                    shouldEnqueue |= checkInitNeighbor(origins, pixelIndex + 1);
                }
                if (y > 0) {
                    shouldEnqueue |= checkInitNeighbor(origins, pixelIndex - width);
                }
                if (y < height - 1) {
                    shouldEnqueue |= checkInitNeighbor(origins, pixelIndex + width);
                }

                if (shouldEnqueue) {
                    queue.enqueue(encodeEntry(0, pixelIndex));
                    origins[pixelIndex] = encodeOrigin(0, x, y);
                }
            }

            if (++x == width) {
                x = 0;
                y++;
            }
        }

        // perform propagation until the queue is empty
        while (!queue.isEmpty()) {
            long entry = queue.dequeueLong();
            int currentIndex = decodeEntryIndex(entry);
            var currentOrigin = origins[currentIndex];

            // if the distance in the queue entry is higher than the current distance,
            // this means it was enqueued multiple times with decreasing distances.
            // This queue entry can be ignored since we must have already processed it with a lower distance.
            if (decodeEntryDistance(entry) > decodeOriginDistance(currentOrigin)) {
                continue;
            }

            int x = currentIndex % width;
            int y = currentIndex / width;
            var currentOriginX = decodeOriginX(currentOrigin);
            var currentOriginY = decodeOriginY(currentOrigin);

            // check and perform propagation to neighbors
            if (x > 0) {
                propagate(origins, queue, currentOriginX, currentOriginY, currentIndex - 1, x - 1, y);
            }
            if (x < width - 1) {
                propagate(origins, queue, currentOriginX, currentOriginY, currentIndex + 1, x + 1, y);
            }
            if (y > 0) {
                propagate(origins, queue, currentOriginX, currentOriginY, currentIndex - width, x, y - 1);
            }
            if (y < height - 1) {
                propagate(origins, queue, currentOriginX, currentOriginY, currentIndex + width, x, y + 1);
            }

            // this block optionally does 8-neighborhood propagation
//            if (x > 0 && y > 0) {
//                propagate(origins, queue, currentOriginX, currentOriginY, currentIndex - width - 1, x - 1, y - 1);
//            }
//            if (x < width - 1 && y > 0) {
//                propagate(origins, queue, currentOriginX, currentOriginY, currentIndex - width + 1, x + 1, y - 1);
//            }
//            if (x > 0 && y < height - 1) {
//                propagate(origins, queue, currentOriginX, currentOriginY, currentIndex + width - 1, x - 1, y + 1);
//            }
//            if (x < width - 1 && y < height - 1) {
//                propagate(origins, queue, currentOriginX, currentOriginY, currentIndex + width + 1, x + 1, y + 1);
//            }
        }

        // copy each transparent pixel's color from its calculated closest non-transparent pixel (the "origin")
        for (int pixelIndex = 0; pixelIndex < pixelCount; pixelIndex++) {
            long origin = origins[pixelIndex];
            if (origin == UNVISITED) {
                throw new AssertionError("unvisited pixel");
            }

            if (origin == OPAQUE) {
//                pixels[pixelIndex] = 0xFFFF0000; // red
                continue;
            }

            int oldColor = MemoryUtil.memGetInt(ppPixel + (pixelIndex * 4L));
            if (ColorABGR.unpackAlpha(oldColor) > 0) {
                continue;
            }

            int originIndex = decodeOriginX(origin) + decodeOriginY(origin) * width;

            // write only the color but preserve the current alpha
//             pixels[pixelIndex] = (pixels[originIndex] & 0x00FFFFFF) | (pixels[pixelIndex] & 0xFF000000);
//            pixels[pixelIndex] = (pixels[originIndex] & 0x00FFFFFF) | 0xFF000000;
//            if (unpackAlpha(pixels[pixelIndex]) > 0) {
//                // pixels[pixelIndex] = 0xFF00FF00; // green
//            } else {
//                pixels[pixelIndex] = (pixels[originIndex] & 0x00FFFFFF) | 0xFF000000;
//            }

//            pixels[pixelIndex] = (pixels[originIndex] & 0x00FFFFFF) | (pixels[pixelIndex] & 0xFF000000);
            int originColor = MemoryUtil.memGetInt(ppPixel + (originIndex * 4L));
            MemoryUtil.memPutInt(ppPixel + (pixelIndex * 4L), (originColor & 0x00FFFFFF) | (oldColor & 0xFF000000));
        }

    }

    @Unique
    private static boolean checkInitNeighbor(long[] origins, int pixelIndex) {
        return origins[pixelIndex] == UNVISITED;
    }

    /**
     * Checks the origin of the current pixel is closer to the neighbor than its current origin. If this is the case or
     * the neighbor is unvisited, the origin is propagated and the neighbor is enqueued.
     */
    @Unique
    private static void propagate(long[] origins, LongHeapPriorityQueue queue, int currentOriginX, int currentOriginY, int neighborIndex, int neighborX, int neighborY) {
        long neighborOrigin = origins[neighborIndex];
        if (neighborOrigin == OPAQUE) {
            return;
        }

        // calculate the distance between the neighbor and the current pixel's origin
        float dx = (float) Math.abs(neighborX - currentOriginX);
        float dy = (float) Math.abs(neighborY - currentOriginY);
        float newDistance = dx * dx * dx + dy * dy * dy;
        if (neighborOrigin == UNVISITED || newDistance < decodeOriginDistance(neighborOrigin)) {
            origins[neighborIndex] = encodeOrigin(newDistance, currentOriginX, currentOriginY);
            queue.enqueue(encodeEntry(newDistance, neighborIndex));
        }
    }
}