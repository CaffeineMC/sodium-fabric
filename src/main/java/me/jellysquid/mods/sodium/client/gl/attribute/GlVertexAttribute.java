package me.jellysquid.mods.sodium.client.gl.attribute;

import org.apache.commons.lang3.EnumUtils;

import java.util.EnumMap;

public class GlVertexAttribute {
    private final int format;
    private final int count;
    private final int pointer;
    private final int size;

    private final boolean normalized;

    public GlVertexAttribute(GlVertexAttributeFormat format, int count, boolean normalized, int pointer) {
        this.format = format.getGlFormat();
        this.size = format.getSize() * count;

        this.count = count;
        this.normalized = normalized;
        this.pointer = pointer;
    }

    public static <T extends Enum<T>> GlVertexAttribute.Builder<T> builder(Class<T> type) {
        return new Builder<>(type);
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

    public static class Builder<T extends Enum<T>> {
        private final EnumMap<T, GlVertexAttribute> attributes;
        private final Class<T> type;

        private int pointer;

        public Builder(Class<T> type) {
            this.type = type;
            this.attributes = new EnumMap<>(type);
        }

        public Builder<T> add(T type, GlVertexAttribute attribute) {
            if (this.attributes.put(type, attribute) != null) {
                throw new IllegalStateException("Attribute already defined");
            }

            return this;
        }

        public GlVertexFormat<T> build(int stride) {
            int size = 0;

            for (T key : EnumUtils.getEnumList(this.type)) {
                GlVertexAttribute attribute = this.attributes.get(key);

                if (attribute == null) {
                    throw new NullPointerException("Attribute not assigned to key " + key.name());
                }

                size += attribute.getSize();
            }

            if (stride < size) {
                throw new IllegalArgumentException("Stride is too small");
            }

            return new GlVertexFormat<>(this.attributes, stride);
        }
    }
}
