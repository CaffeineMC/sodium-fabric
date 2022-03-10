package net.caffeinemc.gfx.api.types;

public enum IntType {
    UNSIGNED_BYTE(1),
    UNSIGNED_SHORT(2),
    UNSIGNED_INT(4);

    private final int size;

    IntType(int size) {
        this.size = size;
    }

    public int getSize() {
        return this.size;
    }
}
