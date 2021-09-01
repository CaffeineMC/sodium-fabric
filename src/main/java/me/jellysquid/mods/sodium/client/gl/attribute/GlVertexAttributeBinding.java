package me.jellysquid.mods.sodium.client.gl.attribute;

import me.jellysquid.mods.sodium.client.gl.shader.ShaderBindingPoint;

public class GlVertexAttributeBinding extends GlVertexAttribute {
    private final int index;
    private final boolean isIntegerType;

    public GlVertexAttributeBinding(ShaderBindingPoint bindingPoint, GlVertexAttribute attribute) {
        this(bindingPoint, attribute, false);
    }

    public GlVertexAttributeBinding(ShaderBindingPoint bindingPoint, GlVertexAttribute attribute, boolean isIntegerType) {
        super(attribute.getFormat(), attribute.getSize(), attribute.getCount(), attribute.isNormalized(), attribute.getPointer(), attribute.getStride());

        this.index = bindingPoint.genericAttributeIndex();
        this.isIntegerType = isIntegerType;
    }

    public int getIndex() {
        return this.index;
    }

    public boolean isInteger() {
        return this.isIntegerType;
    }
}
