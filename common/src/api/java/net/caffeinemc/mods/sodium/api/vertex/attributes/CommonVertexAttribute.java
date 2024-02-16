package net.caffeinemc.mods.sodium.api.vertex.attributes;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

public enum CommonVertexAttribute {
    POSITION(DefaultVertexFormat.ELEMENT_POSITION),
    COLOR(DefaultVertexFormat.ELEMENT_COLOR),
    TEXTURE(DefaultVertexFormat.ELEMENT_UV0),
    OVERLAY(DefaultVertexFormat.ELEMENT_UV1),
    LIGHT(DefaultVertexFormat.ELEMENT_UV2),
    NORMAL(DefaultVertexFormat.ELEMENT_NORMAL);

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
        return this.element.getByteSize();
    }
}
