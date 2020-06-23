package me.jellysquid.mods.sodium.client.gl.util;

public class BufferSlice {
    public final int start, len;

    public BufferSlice(int start, int len) {
        this.start = start;
        this.len = len;
    }

    public static long pack(int start, int len) {
        return (long) start & 0xffffffffL | ((long) len & 0xffffffffL) << 32;
    }

    public static int unpackStart(long slice) {
        return (int) (slice & 0xffffffffL);
    }

    public static int unpackLength(long slice) {
        return (int) (slice >>> 32 & 0xffffffffL);
    }
}
