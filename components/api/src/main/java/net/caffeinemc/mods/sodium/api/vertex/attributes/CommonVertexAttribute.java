package net.caffeinemc.mods.sodium.api.vertex.attributes;

import net.minecraft.client.render.VertexFormatElement;
import net.minecraft.client.render.VertexFormats;

public enum CommonVertexAttribute {
    POSITION(VertexFormats.POSITION_ELEMENT),
    COLOR(VertexFormats.COLOR_ELEMENT),
    TEXTURE(VertexFormats.TEXTURE_ELEMENT),
    OVERLAY(VertexFormats.OVERLAY_ELEMENT),
    LIGHT(VertexFormats.LIGHT_ELEMENT),
    NORMAL(VertexFormats.NORMAL_ELEMENT);

    private final VertexFormatElement element;

    public static final int COUNT = CommonVertexAttribute.values().length;

    CommonVertexAttribute(VertexFormatElement element) {
        this.element = element;
    }

    public static CommonVertexAttribute getCommonType(VertexFormatElement element) {
        for (var type : CommonVertexAttribute.values()) {
            if (type.element == element) {
                return type;
            }
        }

        return null;
    }

    public int getByteLength() {
        return this.element.getByteLength();
    }
}
