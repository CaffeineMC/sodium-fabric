package me.jellysquid.mods.thingl.attribute;

public class VertexAttributeBinding extends VertexAttribute {
    private final int index;
    private final boolean isIntegerType;

    public VertexAttributeBinding(int index, VertexAttribute attribute) {
        this(index, attribute, false);
    }

    public VertexAttributeBinding(int index, VertexAttribute attribute, boolean isIntegerType) {
        super(attribute.getFormat(), attribute.getSize(), attribute.getCount(), attribute.isNormalized(), attribute.getPointer(), attribute.getStride());

        this.index = index;
        this.isIntegerType = isIntegerType;
    }

    public int getIndex() {
        return this.index;
    }

    public boolean isInteger() {
        return this.isIntegerType;
    }
}
