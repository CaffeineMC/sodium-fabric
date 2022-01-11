package me.jellysquid.mods.sodium.opengl.attribute;

import me.jellysquid.mods.sodium.render.vertex.type.BufferVertexFormat;

import java.util.EnumMap;

/**
 * Provides a generic vertex format which contains the attributes defined by {@param T}. Other code can then retrieve
 * the attributes and work with encoded data in a generic manner without needing to rely on a specific format.
 *
 * @param <T> The enumeration over the vertex attributes
 */
public class VertexFormat<T extends Enum<T>> implements BufferVertexFormat {
    private final Class<T> attributeEnum;
    private final EnumMap<T, VertexAttribute> attributesKeyed;

    private final int stride;

    public VertexFormat(Class<T> attributeEnum, EnumMap<T, VertexAttribute> attributesKeyed, int stride) {
        this.attributeEnum = attributeEnum;
        this.attributesKeyed = attributesKeyed;
        this.stride = stride;
    }

    public static <T extends Enum<T>> Builder<T> builder(Class<T> type, int stride) {
        return new Builder<>(type, stride);
    }

    /**
     * Returns the {@link VertexAttribute} of this vertex format bound to the type {@param name}.
     * @throws NullPointerException If the attribute does not exist in this format
     */
    public VertexAttribute getAttribute(T name) {
        VertexAttribute attr = this.attributesKeyed.get(name);

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

    public static class Builder<T extends Enum<T>> {
        private final EnumMap<T, VertexAttribute> attributes;
        private final Class<T> type;
        private final int stride;

        public Builder(Class<T> type, int stride) {
            this.type = type;
            this.attributes = new EnumMap<>(type);
            this.stride = stride;
        }

        public Builder<T> addElement(T type, int pointer, VertexAttributeFormat format, int count, boolean normalized, boolean intType) {
            return this.addElement(type, new VertexAttribute(format, count, normalized, pointer, intType));
        }

        /**
         * Adds an vertex attribute which will be bound to the given generic attribute type.
         *
         * @param type The generic attribute type
         * @param attribute The attribute to bind
         * @throws IllegalStateException If an attribute is already bound to the generic type
         */
        private Builder<T> addElement(T type, VertexAttribute attribute) {
            if (attribute.getOffset() >= this.stride) {
                throw new IllegalArgumentException("Element starts outside vertex format");
            }

            if (attribute.getOffset() + attribute.getSize() > this.stride) {
                throw new IllegalArgumentException("Element extends outside vertex format");
            }

            if (this.attributes.put(type, attribute) != null) {
                throw new IllegalStateException("Generic attribute " + type.name() + " already defined in vertex format");
            }

            return this;
        }

        /**
         * Creates a {@link VertexFormat} from the current builder.
         */
        public VertexFormat<T> build() {
            int size = 0;

            for (T key : this.type.getEnumConstants()) {
                VertexAttribute attribute = this.attributes.get(key);

                if (attribute == null) {
                    throw new NullPointerException("Generic attribute not assigned to enumeration " + key.name());
                }

                size = Math.max(size, attribute.getOffset() + attribute.getSize());
            }

            // The stride must be large enough to cover all attributes. This still allows for additional padding
            // to be added to the end of the vertex to accommodate alignment restrictions.
            if (this.stride < size) {
                throw new IllegalArgumentException("Stride is too small");
            }

            return new VertexFormat<>(this.type, this.attributes, this.stride);
        }
    }
}
