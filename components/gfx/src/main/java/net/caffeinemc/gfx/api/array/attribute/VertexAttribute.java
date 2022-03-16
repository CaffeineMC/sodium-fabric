package net.caffeinemc.gfx.api.array.attribute;

/**
 * @param format     The format used
 * @param count      The number of components in the vertex attribute
 * @param normalized Specifies whether or not fixed-point data values should be normalized (true) or used directly
 *                   as fixed-point values (false)
 * @param offset     The offset to the first component in the attribute
 * @param intType    Whether to treat the attribute as a pure integer value
 */
public record VertexAttribute(VertexAttributeFormat format, int count,
                              boolean normalized, int offset, boolean intType) {

    public int length() {
        return this.format().size() * this.count();
    }
}
