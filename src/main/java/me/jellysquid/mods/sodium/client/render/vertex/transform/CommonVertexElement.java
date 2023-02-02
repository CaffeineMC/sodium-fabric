package me.jellysquid.mods.sodium.client.render.vertex.transform;

import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormatElement;
import net.minecraft.client.render.VertexFormats;

import java.util.Arrays;

public enum CommonVertexElement {
    POSITION,
    COLOR,
    TEXTURE,
    OVERLAY,
    LIGHT,
    NORMAL;

    public static final int COUNT = CommonVertexElement.values().length;

    public static CommonVertexElement getCommonType(VertexFormatElement element) {
        if (element == VertexFormats.POSITION_ELEMENT) {
            return POSITION;
        }

        if (element == VertexFormats.COLOR_ELEMENT) {
            return COLOR;
        }

        if (element == VertexFormats.TEXTURE_ELEMENT) {
            return TEXTURE;
        }

        if (element == VertexFormats.OVERLAY_ELEMENT) {
            return OVERLAY;
        }

        if (element == VertexFormats.LIGHT_ELEMENT) {
            return LIGHT;
        }

        if (element == VertexFormats.NORMAL_ELEMENT) {
            return NORMAL;
        }

        return null;
    }

    public static int[] getOffsets(VertexFormat format) {
        var results = new int[CommonVertexElement.COUNT];

        Arrays.fill(results, -1);

        var elements = format.getElements();
        var offsets = format.offsets;

        for (int i = 0; i < elements.size(); i++) {
            var element = elements.get(i);
            var offset = offsets.getInt(i);

            var type = CommonVertexElement.getCommonType(element);

            if (type != null) {
                results[type.ordinal()] = offset;
            }
        }

        return results;
    }
}
