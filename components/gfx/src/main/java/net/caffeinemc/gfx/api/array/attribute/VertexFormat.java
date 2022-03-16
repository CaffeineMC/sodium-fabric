package net.caffeinemc.gfx.api.array.attribute;

import net.caffeinemc.gfx.api.buffer.BufferVertexFormat;

import java.util.EnumMap;

/**
 * Provides a generic vertex format which contains the attributes defined by {@param T}. Other code can then retrieve
 * the attributes and work with encoded data in a generic manner without needing to rely on a specific format.
 *
 * @param <T> The enumeration over the vertex attributes
 */
public class VertexFormat<T extends Enum<T>> implements BufferVertexFormat {
    private final Class<T> genericType;
    private final EnumMap<T, VertexAttribute> bindings;

    private final int stride;

    public VertexFormat(Class<T> genericType, EnumMap<T, VertexAttribute> bindings, int stride) {
        this.genericType = genericType;
        this.bindings = bindings;
        this.stride = stride;
    }

    /**
     * Returns the {@link VertexAttribute} of this vertex format bound to the type {@param name}.
     * @throws NullPointerException If the attribute does not exist in this format
     */
    public VertexAttribute getAttribute(T type) {
        VertexAttribute attrib = this.bindings.get(type);

        if (attrib == null) {
            throw new NullPointerException("No attribute exists for " + type.toString());
        }

        return attrib;
    }

    /**
     * @return The stride (or the size of) the vertex format in bytes
     */
    @Override
    public int stride() {
        return this.stride;
    }

    @Override
    public String toString() {
        return String.format("GlVertexFormat<%s>{attributes=%d,stride=%d}", this.genericType.getName(),
                this.bindings.size(), this.stride);
    }

    public static <T extends Enum<T>> Builder<T> builder(Class<T> type, int stride) {
        return new Builder<>(type, stride);
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
         * Adds a attribute for this format which will be bound to the given generic attribute type.
         *
         * @param type The generic attribute type
         * @param attribute The attribute to be bound to the generic type
         *
         * @throws IllegalStateException If an attribute is already bound to the generic type
         * @throws IllegalArgumentException If the attribute is specified outside the byte boundaries of this format
         */
        private Builder<T> addElement(T type, VertexAttribute attribute) {
            // The vertex format must be large enough to cover all attributes. This still allows for additional padding
            // to be added to the end of the vertex to accommodate alignment restrictions.
            if (attribute.offset() + attribute.length() > this.stride) {
                throw new IllegalArgumentException("Attribute is not contained within the byte boundaries of this format");
            }

            if (this.attributes.put(type, attribute) != null) {
                throw new IllegalStateException("Generic attribute " + type.name() + " already defined in vertex format");
            }

            return this;
        }

        /**
         * Creates a {@link VertexFormat} from the current builder. All generic attributes must be bound before the
         * vertex format can be created.
         *
         * @throws IllegalStateException If any generic attributes are not bound
         */
        public VertexFormat<T> build() {
            for (T key : this.type.getEnumConstants()) {
                if (!this.attributes.containsKey(key)) {
                    throw new IllegalStateException("Generic attribute not assigned to enumeration " + key.name());
                }
            }

            return new VertexFormat<>(this.type, this.attributes, this.stride);
        }
    }
}
