package me.jellysquid.mods.sodium.client.gl.util;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;

import java.nio.Buffer;

/**
 * Contains versions of MemoryUtil#memFree that hopefully work on all versions of LWJGL 3 released
 * in the past few years.
 *
 * LWJGL 3.2.3 made breaking changes to memFree, which is why this class is needed:
 * https://github.com/LWJGL/lwjgl3/releases/tag/3.2.3
 */
public class MemoryUtilHelper {
    // memFree for custom / pointer buffers *was* changed.
    public static void memFree(@Nullable PointerBuffer ptr) {
        if (ptr != null) {
            MemoryUtil.nmemFree(ptr.address0());
        }
    }

    // memFree for normal buffers was not changed.
    public static void memFree(@Nullable Buffer ptr) {
        MemoryUtil.memFree(ptr);
    }
}