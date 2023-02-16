package me.jellysquid.mods.sodium.client.util;

import java.util.Arrays;

public class BitArray {
    private static final int ADDRESS_BITS_PER_WORD = 6;
    private static final int BITS_PER_WORD = 1 << ADDRESS_BITS_PER_WORD;
    private static final int BIT_INDEX_MASK = BITS_PER_WORD - 1;

    private BitArray()
    {

    }

    public static long[] create(int count) {
        return new long[(MathUtil.align(count, BITS_PER_WORD) >> ADDRESS_BITS_PER_WORD)];
    }

    public static boolean get(long[] words, int index) {
        return (words[wordIndex(index)] & 1L << bitIndex(index)) != 0;
    }

    public static void set(long[] words, int index) {
        words[wordIndex(index)] |= 1L << bitIndex(index);
    }

    public static void unset(long[] words, int index) {
        words[wordIndex(index)] &= ~(1L << bitIndex(index));
    }

    public static void put(long[] words, int index, boolean value) {
        int wordIndex = wordIndex(index);
        int bitIndex = bitIndex(index);
        long intValue = value ? 1 : 0;
        words[wordIndex] = (words[wordIndex] & ~(1L << bitIndex)) | (intValue << bitIndex);
    }

    public static int count(long[] words) {
        int sum = 0;

        for (long word : words) {
            sum += Long.bitCount(word);
        }

        return sum;
    }

    public static void clear(long[] words) {
        Arrays.fill(words, 0L);
    }

    private static int wordIndex(int index) {
        return index >> ADDRESS_BITS_PER_WORD;
    }

    private static int bitIndex(int index) {
        return index & BIT_INDEX_MASK;
    }


    public static int getPair(long[] words, int index) {
        return (int) ((words[index>>5] >> ((index&0b11111)<<1))&3);
    }

    public static void setPair(long[] words, int index, int pair) {
        long w = words[index>>5];
        int i = (index&0b11111)<<1;
        long v = ((long)pair)<<(i);
        w &= ~v;
        w |= v;
        words[index>>5] = w;
    }
}
