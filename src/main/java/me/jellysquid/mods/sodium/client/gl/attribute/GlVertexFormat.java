package me.jellysquid.mods.sodium.client.gl.attribute;

import java.util.EnumMap;

/**
 * Provides a generic vertex format which contains the attributes defined by {@param T}. Other code can then retrieve
 * the attributes and work with encoded data in a generic manner without needing to rely on a specific format.
 *
 * @param <T> The enumeration over the vertex attributes
 */
public class GlVertexFormat<T extends Enum<T>> implements BufferVertexFormat {
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

    public static <T extends Enum<T>> Builder<T> builder(Class<T> type, int stride) {
        return new Builder<>(type, stride);
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

    public static class Builder<T extends Enum<T>> {
        private final EnumMap<T, GlVertexAttribute> attributes;
        private final Class<T> type;
        private final int stride;

        public Builder(Class<T> type, int stride) {
            this.type = type;
            this.attributes = new EnumMap<>(type);
            this.stride = stride;
        }

        public Builder<T> addElement(T type, int pointer, GlVertexAttributeFormat format, int count, boolean normalized) {
            return this.addElement(type, new GlVertexAttribute(format, count, normalized, pointer, this.stride));
        }

        /**
         * Adds an vertex attribute which will be bound to the given generic attribute type.
         *
         * @param type The generic attribute type
         * @param attribute The attribute to bind
         * @throws IllegalStateException If an attribute is already bound to the generic type
         */
        private Builder<T> addElement(T type, GlVertexAttribute attribute) {
            if (attribute.getPointer() >= this.stride) {
                throw new IllegalArgumentException("Element starts outside vertex format");
            }

            if (attribute.getPointer() + attribute.getSize() > this.stride) {
                throw new IllegalArgumentException("Element extends outside vertex format");
            }

            if (this.attributes.put(type, attribute) != null) {
                throw new IllegalStateException("Generic attribute " + type.name() + " already defined in vertex format");
            }

            return this;
        }

        /**
         * Creates a {@link GlVertexFormat} from the current builder.
         */
        public GlVertexFormat<T> build() {
            int size = 0;

            for (T key : this.type.getEnumConstants()) {
                GlVertexAttribute attribute = this.attributes.get(key);

                if (attribute == null) {
                    throw new NullPointerException("Generic attribute not assigned to enumeration " + key.name());
                }

                size = Math.max(size, attribute.getPointer() + attribute.getSize());
            }

            // The stride must be large enough to cover all attributes. This still allows for additional padding
            // to be added to the end of the vertex to accommodate alignment restrictions.
            if (this.stride < size) {
                throw new IllegalArgumentException("Stride is too small");
            }

            return new GlVertexFormat<>(this.type, this.attributes, this.stride);
        }
    }
}
