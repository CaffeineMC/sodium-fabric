package net.caffeinemc.mods.sodium.api.vertex.attributes;

import net.minecraft.client.render.VertexFormatElement;
import net.minecraft.client.render.VertexFormats;

public enum CommonVertexAttribute {
    POSITION(VertexFormatElement.POSITION),
    COLOR(VertexFormatElement.COLOR),
    TEXTURE(VertexFormatElement.UV_0),
    OVERLAY(VertexFormatElement.UV_1),
    LIGHT(VertexFormatElement.UV_2),
    NORMAL(VertexFormatElement.NORMAL);

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
        return this.element.getSizeInBytes();
    }
}
