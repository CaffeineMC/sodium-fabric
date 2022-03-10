package net.caffeinemc.gfx.api.array.attribute;

public class VertexAttributeBinding extends VertexAttribute {
    private final int index;

    public VertexAttributeBinding(int index, VertexAttribute attribute) {
        super(attribute.getFormat(), attribute.getSize(), attribute.getCount(), attribute.isNormalized(), attribute.getOffset(), attribute.isIntType());

        this.index = index;
    }

    public int getIndex() {
        return this.index;
    }
}
