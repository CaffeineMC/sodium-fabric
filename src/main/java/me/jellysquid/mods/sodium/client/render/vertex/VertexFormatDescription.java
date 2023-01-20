package me.jellysquid.mods.sodium.client.render.vertex;

import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormatElement;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

public class VertexFormatDescription {
    public final int id;
    public final int stride;

    public final VertexFormatElement[] elements;
    public final int[] offsets;

    public VertexFormatDescription(VertexFormat format, int id) {
        this.id = id;
        this.elements = format.getElements()
                .toArray(VertexFormatElement[]::new);
        this.offsets = format.offsets
                .toIntArray();
        this.stride = format.getVertexSizeByte();
    }

    public int getOffset(VertexFormatElement element) {
        for (int i = 0; i < this.elements.length; i++) {
            if (element == this.elements[i]) {
                return this.offsets[i];
            }
        }

        throw new NoSuchElementException("Vertex format does not contain element: " + element);
    }

    @Override
    public String toString() {
        return Arrays.stream(this.elements)
                .map(e -> String.format("[%s]", e))
                .collect(Collectors.joining(","));
    }
}
