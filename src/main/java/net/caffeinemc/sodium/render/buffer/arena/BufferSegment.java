package net.caffeinemc.sodium.render.buffer.arena;

public class BufferSegment {
    
    public static final long INVALID = createKey(0, -1);
    
    /**
     * Creates a buffer segment key, which holds the offset and length.
     *
     * @param length The unsigned length of the segment
     * @param offset The unsigned offset of the beginning of the segment into the buffer
     */
    public static long createKey(int length, int offset) {
        return ((long) length << Integer.SIZE) | Integer.toUnsignedLong(offset);
    }
    
    public static int getLength(long key) {
        return (int) (key >>> Integer.SIZE);
    }
    
    public static int getOffset(long key) {
        return (int) key;
    }
    
    /**
     * Returns the end (exclusive) of the segment.
     */
    public static int getEnd(long key) {
        return getOffset(key) + getLength(key);
    }
    
    public static int compareLengthOffset(long key1, long key2) {
        // if we know that 0b1xxx is larger than any 0b0xxx, we know that length will always be prioritized over offset
        return Long.compareUnsigned(key1, key2);
    }
    
    public static int compareOffsetLength(long key1, long key2) {
        // same strategy as compareLengthOffset, but we flop the offset and length
        return Long.compareUnsigned(
                (key1 >>> Integer.SIZE) | (key1 << Integer.SIZE),
                (key2 >>> Integer.SIZE) | (key2 << Integer.SIZE)
        );
    }
    
    public static int compareOffset(long key1, long key2) {
        return Integer.compareUnsigned(getOffset(key1), getOffset(key2));
    }
    
    public static int compareLength(long key1, long key2) {
        return Integer.compareUnsigned(getLength(key1), getLength(key2));
    }
}
