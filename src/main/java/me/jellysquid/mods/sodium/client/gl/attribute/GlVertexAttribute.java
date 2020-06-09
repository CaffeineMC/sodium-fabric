package me.jellysquid.mods.sodium.client.gl.attribute;

import java.util.EnumMap;

public class GlVertexAttribute {
    private final int index;
    private final int format;
    private final int count;
    private final int pointer;
    private final int size;
    private final int stride;

    private final boolean normalized;

    /**
     * @param format The format used
     * @param count The number of components in the vertex attribute
     * @param normalized Specifies whether or not fixed-point data values should be normalized (true) or used directly
     *                   as fixed-point values (false)
     * @param pointer The offset to the first component in the attribute
     */
    public GlVertexAttribute(int index, GlVertexAttributeFormat format, int count, boolean normalized, int pointer, int stride) {
        this.index = index;
        this.format = format.getGlFormat();
        this.size = format.getSize() * count;

        this.count = count;
        this.normalized = normalized;
        this.pointer = pointer;
        this.stride = stride;
    }

    public static <T extends Enum<T>> GlVertexAttribute.Builder<T> builder(Class<T> type, int stride) {
        return new Builder<>(type, stride);
    }

    public int getIndex() {
        return this.index;
    }

    public long getSize() {
        return this.size;
    }

    public int getPointer() {
        return this.pointer;
    }

    public int getCount() {
        return this.count;
    }

    public int getFormat() {
        return this.format;
    }

    public boolean isNormalized() {
        return this.normalized;
    }

    public int getStride() {
        return this.stride;
    }

    public boolean isFormatSupported() {
        return this.format != GlVertexAttributeFormat.UNSUPPORTED_FORMAT;
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
            return this.addElement(type, new GlVertexAttribute(this.attributes.size(), format, count, normalized, pointer, this.stride));
        }

        /**
         * Adds an vertex attribute which will be bound to the given generic attribute type.
         *
         * @param type The generic attribute type
         * @param attribute The attribute to bind
         * @throws IllegalStateException If an attribute is already bound to the generic type
         */
        private Builder<T> addElement(T type, GlVertexAttribute attribute) {
            if (attribute.pointer >= this.stride) {
                throw new IllegalArgumentException("Element starts outside vertex format");
            }

            if (attribute.pointer + attribute.size > this.stride) {
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

                size = Math.max(size, (int) (attribute.getPointer() + attribute.getSize()));
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
