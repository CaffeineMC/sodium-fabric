package me.jellysquid.mods.sodium.client.util;

import sun.nio.ch.DirectBuffer;

import java.nio.ByteBuffer;

public class BufferUtil {
    private static final boolean USE_UNSAFE = UnsafeUtil.isAvailable();

    public static void copyIntArray(int[] data, int limit, int offset, ByteBuffer buffer) {
        if (limit > data.length) {
            throw new IllegalArgumentException("Source array is too small");
        }

        int bytes = limit * 4;

        if (buffer.capacity() - offset < bytes) {
            throw new IllegalArgumentException("Destination buffer is too small");
        }

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
