package me.jellysquid.mods.sodium.client.render.chunk;

import it.unimi.dsi.fastutil.objects.*;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ChunkRenderList {
    private final Reference2ObjectLinkedOpenHashMap<RenderRegion, List<RenderSection>> entries = new Reference2ObjectLinkedOpenHashMap<>();

    public Iterable<Map.Entry<RenderRegion, List<RenderSection>>> sorted(boolean reverse) {
        if (this.entries.isEmpty()) {
            return Collections.emptyList();
        }

        Reference2ObjectSortedMap.FastSortedEntrySet<RenderRegion, List<RenderSection>> entries =
                this.entries.reference2ObjectEntrySet();

        if (reverse) {
            return () -> new Iterator<>() {
                final ObjectBidirectionalIterator<Reference2ObjectMap.Entry<RenderRegion, List<RenderSection>>> iterator =
                        entries.fastIterator(entries.last());

                @Override
                public boolean hasNext() {
                    return this.iterator.hasPrevious();
                }

                @Override
                public Map.Entry<RenderRegion, List<RenderSection>> next() {
                    return this.iterator.previous();
                }
            };
        } else {
            return () -> new Iterator<>() {
                final ObjectBidirectionalIterator<Reference2ObjectMap.Entry<RenderRegion, List<RenderSection>>> iterator =
                        entries.fastIterator();

                @Override
                public boolean hasNext() {
                    return this.iterator.hasNext();
                }

                @Override
                public Map.Entry<RenderRegion, List<RenderSection>> next() {
                    return this.iterator.next();
                }
            };
        }
    }

    public void clear() {
        this.entries.clear();
    }

    public void add(RenderSection render) {
        RenderRegion region = render.getRegion();

        List<RenderSection> sections = this.entries.computeIfAbsent(region, (key) -> new ObjectArrayList<>());
        sections.add(render);
    }

    public int getCount() {
        return this.entries.values()
                .stream()
                .mapToInt(List::size)
                .sum();
    }
}
