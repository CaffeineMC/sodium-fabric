package me.jellysquid.mods.sodium.client.util;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class UnsafeUtil {
    private static final Unsafe UNSAFE;

    public static final long INT_ARRAY_OFFSET;

    static {
        UNSAFE = findUnsafe();

        if (UNSAFE != null) {
            INT_ARRAY_OFFSET = UNSAFE.arrayBaseOffset(int[].class);
        } else {
            INT_ARRAY_OFFSET = -1;
        }
    }

    private static Unsafe findUnsafe() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);

            return (Unsafe) field.get(null);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    public static boolean isAvailable() {
        return UNSAFE != null;
    }

    public static Unsafe instance() {
        if (!isAvailable()) {
            throw new UnsupportedOperationException("Unsafe is not available on this platform");
        }

        return UNSAFE;
    }
}
