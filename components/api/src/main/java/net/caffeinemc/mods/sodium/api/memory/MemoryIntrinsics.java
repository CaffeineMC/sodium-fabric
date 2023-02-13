package net.caffeinemc.mods.sodium.api.memory;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class MemoryIntrinsics {
    private static final Unsafe UNSAFE;

    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);

            UNSAFE = (Unsafe) field.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Couldn't obtain reference to sun.misc.Unsafe", e);
        }
    }

    /**
     * Copies the number of bytes specified by {@param length} between off-heap buffers {@param src} and {@param dst}.
     * <p>
     * WARNING: This function makes no attempt to verify that the parameters are correct. If you pass invalid pointers
     * or read/write memory outside a buffer, the JVM will likely crash!
     *
     * @param src The source pointer to begin copying from
     * @param dst The destination pointer to begin copying into
     * @param length The number of bytes to copy
     */
    public static void copyMemory(long src, long dst, int length) {
        // This seems to be faster than MemoryUtil.copyMemory in all cases.
        UNSAFE.copyMemory(src, dst, length);
    }
}
