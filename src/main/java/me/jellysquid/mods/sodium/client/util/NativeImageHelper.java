package me.jellysquid.mods.sodium.client.util;

import net.minecraft.client.texture.NativeImage;

import java.util.Locale;

public class NativeImageHelper {
    public static long getPointerRGBA(NativeImage nativeImage) {
        if (nativeImage.getFormat() != NativeImage.Format.RGBA) {
            throw new IllegalArgumentException(String.format(Locale.ROOT,
                    "Tried to get pointer to RGBA pixel data on NativeImage of wrong format; have %s", nativeImage.getFormat()));
        }

        return nativeImage.pointer;
    }
}
