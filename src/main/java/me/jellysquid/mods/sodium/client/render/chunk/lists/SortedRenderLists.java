package me.jellysquid.mods.sodium.client.render.chunk.lists;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.jellysquid.mods.sodium.client.util.ReversibleArrayIterator;

import java.util.Iterator;

public class SortedRenderLists {
    private final ObjectArrayList<ChunkRenderList> sorted;

    public SortedRenderLists(ObjectArrayList<ChunkRenderList> sorted) {
        this.sorted = sorted;
    }

    public ReversibleArrayIterator<ChunkRenderList> sorted(boolean reverse) {
        return new ReversibleArrayIterator<>(this.sorted, reverse);
    }

    public Iterator<ChunkRenderList> sorted() {
        return this.sorted.iterator();
    }
}
