package net.caffeinemc.mods.sodium.api.vertex.attributes;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

public enum CommonVertexAttribute {
    POSITION(VertexFormatElement.POSITION),
    COLOR(VertexFormatElement.COLOR),
    TEXTURE(VertexFormatElement.UV0),
    OVERLAY(VertexFormatElement.UV1),
    LIGHT(VertexFormatElement.UV2),
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
        return this.element.byteSize();
    }
}
