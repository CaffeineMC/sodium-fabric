package me.jellysquid.mods.sodium.client.gl.util;

public class VertexSlice {
    public static final long EMPTY_SLICE = 0L;

    public static long pack(int first, int count) {
        return ((long) first & 0xffffffffL) | (((long) count & 0xffffffffL) << 32);
    }

    public static int unpackFirst(long word) {
        return (int) (word & 0xffffffffL);
    }

    public static int unpackCount(long word) {
        return (int) ((word >>> 32) & 0xffffffffL);
    }

    public static boolean isEmpty(long slice) {
        return slice == EMPTY_SLICE;
    }
}
