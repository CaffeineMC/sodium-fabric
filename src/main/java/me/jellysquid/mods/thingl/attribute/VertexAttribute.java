package me.jellysquid.mods.thingl.attribute;

public class VertexAttribute {
    private final int format;
    private final int count;
    private final int pointer;
    private final int size;

    private final boolean normalized;

    /**
     * @param format The format used
     * @param count The number of components in the vertex attribute
     * @param normalized Specifies whether or not fixed-point data values should be normalized (true) or used directly
 *                   as fixed-point values (false)
     * @param pointer The offset to the first component in the attribute
     */
    public VertexAttribute(VertexAttributeFormat format, int count, boolean normalized, int pointer) {
        this(format.typeId(), format.size() * count, count, normalized, pointer);
    }

    protected VertexAttribute(int format, int size, int count, boolean normalized, int pointer) {
        this.format = format;
        this.size = size;
        this.count = count;
        this.normalized = normalized;
        this.pointer = pointer;
    }

    public int getSize() {
        return this.size;
    }

    public int getPointer() {
        return this.pointer;
    }

    public int getCount() {
        return this.count;
    }

    public int getFormat() {
        return this.format;
    }

    public boolean isNormalized() {
        return this.normalized;
    }
}
