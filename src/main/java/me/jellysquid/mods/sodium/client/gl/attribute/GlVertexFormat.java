package me.jellysquid.mods.sodium.client.gl.attribute;

import me.jellysquid.mods.sodium.client.gl.GlHelper;

import java.util.EnumMap;

public class GlVertexFormat<T extends Enum<T>> {
    private final EnumMap<T, GlVertexAttribute> attributes;
    private final int stride;

    public GlVertexFormat(EnumMap<T, GlVertexAttribute> attributes, int stride) {
        this.attributes = attributes;
        this.stride = stride;
    }

    public GlVertexAttribute getAttribute(T name) {
        return this.attributes.get(name);
    }

    public int getStride() {
        return this.stride;
    }

    public boolean isSupported() {
        for (GlVertexAttribute attribute : this.attributes.values()) {
            if (!GlHelper.isVertexFormatSupported(attribute.getFormat())) {
                return false;
            }
        }

        return true;
    }
}
