package me.jellysquid.mods.sodium.client.util;

import sun.nio.ch.DirectBuffer;

import java.nio.ByteBuffer;

public class BufferUtil {
    // Hoist the computation into a constant static field so the JVM can optimize the branch away
    private static final boolean USE_UNSAFE = UnsafeUtil.isAvailable();

    /**
     * Copies a slice of integers from the source array into the destination buffer at its current position. This exists
     * to facilitate fast copies of integer arrays from heap memory to native memory since the ByteBuffer interface
     * doesn't provide an optimized code path for it.
     *
     * If the destination buffer is a direct buffer to native memory, unsafe code will be used to copy the integer
     * array using intrinsics. Otherwise, the (slower) fallback path will be used which will work in any situation.
     *
     * @param data The source integer array
     * @param limit The number of integers to be copied
     * @param offset The starting offset in the source array to copy from
     * @param buffer The destination buffer
     */
    public static void copyIntArray(int[] data, int limit, int offset, ByteBuffer buffer) {
        if (limit > data.length) {
            throw new IllegalArgumentException("Source array is too small");
        }

        int bytes = limit * 4;

        // Underflow check
        if (buffer.capacity() - offset < bytes) {
            throw new IllegalArgumentException("Destination buffer is too small");
        }

        // Check that unsafe intrinsics can be used and that the byte buffer points to off-heap memory
        if (USE_UNSAFE && buffer instanceof DirectBuffer) {
            copyIntArrayUnsafe(data, limit, offset, (DirectBuffer) buffer);
        } else {
            copyIntArrayDefault(data, limit, offset, buffer);
        }
    }

    private static void copyIntArrayUnsafe(int[] data, int limit, int offset, DirectBuffer buffer) {
        UnsafeUtil.instance().copyMemory(data, UnsafeUtil.INT_ARRAY_OFFSET, null, buffer.address() + offset, limit * 4);
    }

    private static void copyIntArrayDefault(int[] data, int limit, int offset, ByteBuffer buffer) {
        for (int i = 0; i < limit; i++) {
            buffer.putInt(offset + (i * 4), data[i]);
        }
    }
}
