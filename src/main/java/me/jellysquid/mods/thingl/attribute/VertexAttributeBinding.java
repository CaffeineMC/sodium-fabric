package me.jellysquid.mods.thingl.attribute;

public class VertexAttributeBinding {
    private final int index;
    private final VertexAttribute attribute;
    private final boolean isIntegerType;

    public VertexAttributeBinding(int index, VertexAttribute attribute) {
        this(index, attribute, false);
    }

    public VertexAttributeBinding(int index, VertexAttribute attribute, boolean isIntegerType) {
        this.attribute = attribute;
        this.index = index;
        this.isIntegerType = isIntegerType;
    }

    public VertexAttribute getAttribute() {
        return this.attribute;
    }

    public int getIndex() {
        return this.index;
    }

    public boolean isInteger() {
        return this.isIntegerType;
    }
}
