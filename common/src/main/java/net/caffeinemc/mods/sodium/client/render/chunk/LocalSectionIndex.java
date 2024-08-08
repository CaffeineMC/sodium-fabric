package net.caffeinemc.mods.sodium.client.render.chunk;

public class LocalSectionIndex {
    // XZY order
    private static final int X_BITS = 0b111, X_OFFSET = 5, X_MASK = X_BITS << X_OFFSET;
    private static final int Y_BITS = 0b11, Y_OFFSET = 0, Y_MASK = Y_BITS << Y_OFFSET;
    private static final int Z_BITS = 0b111, Z_OFFSET = 2, Z_MASK = Z_BITS << Z_OFFSET;

    public static int pack(int x, int y, int z) {
        return ((x & X_BITS) << X_OFFSET) | ((y & Y_BITS) << Y_OFFSET) | ((z & Z_BITS) << Z_OFFSET);
    }

    // x + 1
    public static int incX(int idx) {
        return (idx & ~X_MASK) | ((idx + (1 << X_OFFSET)) & X_MASK);
    }

    // x - 1
    public static int decX(int idx) {
        return (idx & ~X_MASK) | ((idx - (1 << X_OFFSET)) & X_MASK);
    }

    // y + 1
    public static int incY(int idx) {
        return (idx & ~Y_MASK) | ((idx + (1 << Y_OFFSET)) & Y_MASK);
    }

    // y - 1
    public static int decY(int idx) {
        return (idx & ~Y_MASK) | ((idx - (1 << Y_OFFSET)) & Y_MASK);
    }

    // z + 1
    public static int incZ(int idx) {
        return (idx & ~Z_MASK) | ((idx + (1 << Z_OFFSET)) & Z_MASK);
    }

    // z - 1
    public static int decZ(int idx) {
        return (idx & ~Z_MASK) | ((idx - (1 << Z_OFFSET)) & Z_MASK);
    }

    public static int unpackX(int idx) {
        return (idx >> X_OFFSET) & X_BITS;
    }

    public static int unpackY(int idx) {
        return (idx >> Y_OFFSET) & Y_BITS;
    }

    public static int unpackZ(int idx) {
        return (idx >> Z_OFFSET) & Z_BITS;
    }
}