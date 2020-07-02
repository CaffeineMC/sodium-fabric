package me.jellysquid.mods.sodium.client.util;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class UnsafeUtil {
    private static final boolean SUPPORTED;
    private static boolean AVAILABLE;

    private static final Unsafe UNSAFE;

    /**
     * The byte offset of the first element in an int[]. If unsafe intrinsics are not available on this platform,
     * the value will be -1.
     */
    public static final long INT_ARRAY_OFFSET;

    static {
        UNSAFE = findUnsafe();
        SUPPORTED = UNSAFE != null;

        if (SUPPORTED) {
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
            SodiumClientMod.logger().warn("Could not find Unsafe intrinsics", e);
        }

        return null;
    }

    /**
     * @return True if unsafe intrinsics are available and enabled
     */
    public static boolean isAvailable() {
        return AVAILABLE;
    }

    /**
     * @return True if unsafe intrinsics are supported on this platform
     */
    public static boolean isSupported() {
        return SUPPORTED;
    }

    /**
     * Returns an exposed instance of {@link Unsafe}.
     * @throws UnsupportedOperationException If {@link UnsafeUtil#isAvailable()} is false
     */
    public static Unsafe instance() {
        if (!isAvailable()) {
            throw new UnsupportedOperationException("Unsafe intrinsics are not available");
        }

        return UNSAFE;
    }

    public static Unsafe instanceNullable() {
        return UNSAFE;
    }

    public static void setEnabled(boolean enabled) {
        AVAILABLE = UNSAFE != null && enabled;
    }
}
