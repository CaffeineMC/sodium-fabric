package me.jellysquid.mods.sodium.client.gl.attribute;

import org.lwjgl.opengl.GL20;

import java.util.EnumMap;

/**
 * Provides a generic vertex format which contains the attributes defined by {@param T}. Other code can then retrieve
 * the attributes and work with encoded data in a generic manner without needing to rely on a specific format.
 *
 * @param <T> The enumeration over the vertex attributes
 */
public class GlVertexFormat<T extends Enum<T>> {
    private final Class<T> attributeEnum;
    private final EnumMap<T, GlVertexAttribute> attributesKeyed;
    private final GlVertexAttribute[] attributesArray;

    private final int stride;

    public GlVertexFormat(Class<T> attributeEnum, EnumMap<T, GlVertexAttribute> attributesKeyed, int stride) {
        this.attributeEnum = attributeEnum;
        this.attributesKeyed = attributesKeyed;
        this.attributesArray = attributesKeyed.values().toArray(new GlVertexAttribute[0]);
        this.stride = stride;
    }

    /**
     * Returns the {@link GlVertexAttribute} of this vertex format bound to the type {@param name}.
     * @throws NullPointerException If the attribute does not exist in this format
     */
    public GlVertexAttribute getAttribute(T name) {
        GlVertexAttribute attr = this.attributesKeyed.get(name);

        if (attr == null) {
            throw new NullPointerException("No attribute exists for " + name.toString());
        }

        return attr;
    }

    /**
     * @return The stride (or the size of) the vertex format in bytes
     */
    public int getStride() {
        return this.stride;
    }

    @Override
    public String toString() {
        return String.format("GlVertexFormat<%s>{attributes=%d,stride=%d}", this.attributeEnum.getName(),
                this.attributesKeyed.size(), this.stride);
    }

    public GlVertexAttribute[] getAttributesArray() {
        return this.attributesArray;
    }

    public void enableVertexAttributes() {
        for (GlVertexAttribute binding : this.getAttributesArray()) {
            GL20.glEnableVertexAttribArray(binding.getIndex());
        }
    }

    public void disableVertexAttributes() {
        for (GlVertexAttribute binding : this.getAttributesArray()) {
            GL20.glDisableVertexAttribArray(binding.getIndex());
        }
    }

    public void bindVertexAttributes() {
        for (GlVertexAttribute binding : this.getAttributesArray()) {
            GL20.glVertexAttribPointer(binding.getIndex(), binding.getCount(), binding.getFormat(), binding.isNormalized(), binding.getStride(), binding.getPointer());
        }
    }
}
