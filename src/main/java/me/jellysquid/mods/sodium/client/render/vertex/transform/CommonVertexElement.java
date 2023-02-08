package me.jellysquid.mods.sodium.client.render.vertex.transform;

import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormatElement;
import net.minecraft.client.render.VertexFormats;

import java.util.Arrays;

public enum CommonVertexElement {
    POSITION(VertexFormats.POSITION_ELEMENT),
    COLOR(VertexFormats.COLOR_ELEMENT),
    TEXTURE(VertexFormats.TEXTURE_ELEMENT),
    OVERLAY(VertexFormats.OVERLAY_ELEMENT),
    LIGHT(VertexFormats.LIGHT_ELEMENT),
    NORMAL(VertexFormats.NORMAL_ELEMENT);

    private final VertexFormatElement element;

    public static final int COUNT = CommonVertexElement.values().length;

    CommonVertexElement(VertexFormatElement element) {
        this.element = element;
    }

    public static CommonVertexElement getCommonType(VertexFormatElement element) {
        for (var type : CommonVertexElement.values()) {
            if (type.element == element) {
                return type;
            }
        }

        return null;
    }

    public static int[] getOffsets(VertexFormat format) {
        final int[] offsets = new int[COUNT];

        Arrays.fill(offsets, -1);

        var elements = format.getElements();

        for (int elementIndex = 0; elementIndex < elements.size(); elementIndex++) {
            var element = elements.get(elementIndex);
            var commonType = getCommonType(element);

            if (commonType != null) {
                offsets[commonType.ordinal()] = format.offsets.getInt(elementIndex);
            }
        }

        return offsets;
    }

    public int getByteLength() {
        return this.element.getByteLength();
    }
}
