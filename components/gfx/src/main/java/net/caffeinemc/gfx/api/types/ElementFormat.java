package net.caffeinemc.gfx.api.types;

public enum ElementFormat {
    UNSIGNED_BYTE(1),
    UNSIGNED_SHORT(2),
    UNSIGNED_INT(4);

    private final int size;

    ElementFormat(int size) {
        this.size = size;
    }

    public int getSize() {
        return this.size;
    }

    public static ElementFormat getSmallestType(int count) {
        if ((count & 0xFFFF0000) != 0) {
            return UNSIGNED_INT;
        } else if ((count & 0xFF00) != 0) {
            return UNSIGNED_SHORT;
        } else {
            return UNSIGNED_BYTE;
        }
    }
}
