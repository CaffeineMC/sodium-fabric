package me.jellysquid.mods.sodium.client.render.vertex;

import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import me.jellysquid.mods.sodium.client.render.vertex.transform.CommonVertexElement;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormatElement;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

public class VertexFormatDescription {
    private final VertexFormat format;

    public final int id;
    public final int stride;

    public final int elementCount;
    public final int[] elementOffsets;

    public VertexFormatDescription(VertexFormat format, int id) {
        this.format = format;

        this.id = id;
        this.stride = format.getVertexSizeByte();

        this.elementCount = format.getElements().size();
        this.elementOffsets = CommonVertexElement.getOffsets(format);
    }

    public List<VertexFormatElement> getElements() {
        return this.format.getElements();
    }

    public IntList getOffsets() {
        return IntLists.unmodifiable(this.format.offsets);
    }

    public int getOffset(CommonVertexElement element) {
        int offset = this.elementOffsets[element.ordinal()];

        if (offset == -1) {
            throw new NoSuchElementException("Vertex format does not contain element: " + element);
        }

        return offset;
    }

    @Override
    public String toString() {
        return this.getElements()
                .stream()
                .map(e -> String.format("[%s]", e))
                .collect(Collectors.joining(","));
    }
}
