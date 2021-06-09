package me.jellysquid.mods.sodium.client.gl.util;

public class ElementRange {
    public final int offset, count;
    public final int baseVertex;

    public ElementRange(int offset, int count, int baseVertex) {
        this.offset = offset;
        this.count = count;

        this.baseVertex = baseVertex;
    }
}
