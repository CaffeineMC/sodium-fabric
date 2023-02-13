package me.jellysquid.mods.sodium.client.render.vertex;

import net.caffeinemc.mods.sodium.api.vertex.attributes.CommonVertexAttribute;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatDescription;
import net.minecraft.client.render.VertexFormat;

import java.util.Arrays;
import java.util.NoSuchElementException;

public class VertexFormatDescriptionImpl implements VertexFormatDescription {
    private final int id;
    private final int stride;

    private final int[] offsets;

    public VertexFormatDescriptionImpl(VertexFormat format, int id) {
        this.id = id;
        this.stride = format.getVertexSizeByte();

        this.offsets = getOffsets(format);
    }

    public static int[] getOffsets(VertexFormat format) {
        final int[] offsets = new int[CommonVertexAttribute.COUNT];

        Arrays.fill(offsets, -1);

        var elements = format.getElements();

        for (int elementIndex = 0; elementIndex < elements.size(); elementIndex++) {
            var element = elements.get(elementIndex);
            var commonType = CommonVertexAttribute.getCommonType(element);

            if (commonType != null) {
                offsets[commonType.ordinal()] = format.offsets.getInt(elementIndex);
            }
        }

        return offsets;
    }

    @Override
    public boolean containsElement(CommonVertexAttribute element) {
        return this.offsets[element.ordinal()] != -1;
    }

    @Override
    public int getElementOffset(CommonVertexAttribute element) {
        int offset = this.offsets[element.ordinal()];

        if (offset == -1) {
            throw new NoSuchElementException("Vertex format does not contain element: " + element);
        }

        return offset;
    }

    @Override
    public int id() {
        return this.id;
    }

    @Override
    public int stride() {
        return this.stride;
    }
}
