package me.jellysquid.mods.thingl.attribute;

import me.jellysquid.mods.thingl.shader.ShaderBindingPoint;

public class GlVertexAttributeBinding extends GlVertexAttribute {
    private final int index;
    private final boolean isIntegerType;

    public GlVertexAttributeBinding(int index, GlVertexAttribute attribute) {
        this(index, attribute, false);
    }

    public GlVertexAttributeBinding(int index, GlVertexAttribute attribute, boolean isIntegerType) {
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
