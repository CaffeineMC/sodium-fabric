package me.jellysquid.mods.sodium.client.util;

import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;

import java.util.function.Consumer;

public class SectionIterator {
    private final RenderSection[] lookup;

    private final int[] elements;
    private final int direction;

    private int currentIndex;
    private int remaining;

    public SectionIterator(RenderSection[] lookup, int[] elements, int start, int end, boolean reverse) {
        this.lookup = lookup;

        this.elements = elements;
        this.remaining = end - start;

        this.direction = reverse ? -1 : 1;
        this.currentIndex = reverse ? end - 1 : start;
    }

    public RenderSection next() {
        RenderSection result = null;

        if (this.remaining > 0) {
            result = this.lookup[this.elements[this.currentIndex]];

            this.currentIndex += this.direction;
            this.remaining--;
        }

        return result;
    }

    public void forEach(Consumer<RenderSection> consumer) {
        RenderSection entry;

        while ((entry = this.next()) != null) {
            consumer.accept(entry);
        }
    }
}
