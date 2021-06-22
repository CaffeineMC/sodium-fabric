package me.jellysquid.mods.sodium.client.gl.util;

public class ElementRange {
    public final int elementOffset, elementCount;
    public final int baseVertex;

    public ElementRange(int elementOffset, int elementCount, int baseVertex) {
        this.elementOffset = elementOffset;
        this.elementCount = elementCount;

        this.baseVertex = baseVertex;
    }
}
