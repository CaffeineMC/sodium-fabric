package net.caffeinemc.gfx.api.array.attribute;

/**
 * An enumeration over the supported data formats that can be used for vertex attributes.
 */
public enum VertexAttributeFormat {
    FLOAT(4),
    SHORT(2),
    UNSIGNED_SHORT(2),
    BYTE(1),
    UNSIGNED_BYTE(1),
    INT(4),
    UNSIGNED_INT(4);

    private final int bytes;

    VertexAttributeFormat(int bytes) {
        this.bytes = bytes;
    }

    public int size() {
        return this.bytes;
    }
}
