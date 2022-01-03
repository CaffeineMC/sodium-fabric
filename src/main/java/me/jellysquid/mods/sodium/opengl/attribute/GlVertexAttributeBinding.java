package me.jellysquid.mods.sodium.opengl.attribute;

public class GlVertexAttributeBinding extends GlVertexAttribute {
    private final int index;

    public GlVertexAttributeBinding(int index, GlVertexAttribute attribute) {
        super(attribute.getFormat(), attribute.getSize(), attribute.getCount(), attribute.isNormalized(), attribute.getOffset(), attribute.getStride(), attribute.isIntType());

        this.index = index;
    }

    public int getIndex() {
        return this.index;
    }
}
