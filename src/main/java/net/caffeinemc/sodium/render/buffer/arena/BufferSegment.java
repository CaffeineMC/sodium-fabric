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
}
