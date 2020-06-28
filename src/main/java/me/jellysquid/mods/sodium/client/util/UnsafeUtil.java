package me.jellysquid.mods.sodium.client.util;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class UnsafeUtil {
    private static final boolean USE_UNSAFE = true;
    private static final Unsafe UNSAFE;

    /**
     * The byte offset of the first element in an int[]. If unsafe intrinsics are not available on this platform,
     * the value will be -1.
     */
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
        if (!USE_UNSAFE) {
            return null;
        }

        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);

            return (Unsafe) field.get(null);
        } catch (ReflectiveOperationException e) {
            // TODO: log error
            return null;
        }
    }

    /**
     * @return True if {@link UnsafeUtil#instance()} can be used, otherwise false
     */
    public static boolean isAvailable() {
        return UNSAFE != null;
    }

    /**
     * Returns an exposed instance of {@link Unsafe}, or throws an exception if it is unavailable.
     */
    public static Unsafe instance() {
        if (!isAvailable()) {
            throw new UnsupportedOperationException("Unsafe is not available on this platform");
        }

        return UNSAFE;
    }

    public static Unsafe instanceNullable() {
        return UNSAFE;
    }
}
