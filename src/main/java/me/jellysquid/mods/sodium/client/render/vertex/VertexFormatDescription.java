package me.jellysquid.mods.sodium.client.render.vertex;

import me.jellysquid.mods.sodium.client.render.vertex.transform.CommonVertexElement;
import net.minecraft.client.render.VertexFormat;

import java.util.NoSuchElementException;

public class VertexFormatDescription {
    public final int id;
    public final int stride;

    public final int[] offsets;

    public VertexFormatDescription(VertexFormat format, int id) {
        this.id = id;
        this.stride = format.getVertexSizeByte();

        this.offsets = CommonVertexElement.getOffsets(format);
    }

    public boolean hasElement(CommonVertexElement element) {
        return this.offsets[element.ordinal()] != -1;
    }

    public int getElementOffset(CommonVertexElement element) {
        int offset = this.offsets[element.ordinal()];

        if (offset == -1) {
            throw new NoSuchElementException("Vertex format does not contain element: " + element);
        }

        return offset;
    }
}
