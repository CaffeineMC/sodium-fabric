package me.jellysquid.mods.sodium.client.render.chunk;

import it.unimi.dsi.fastutil.objects.*;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ChunkRenderList {
    private final Reference2ObjectLinkedOpenHashMap<RenderRegion, List<RenderChunk>> entries = new Reference2ObjectLinkedOpenHashMap<>();

    public Iterable<Map.Entry<RenderRegion, List<RenderChunk>>> sorted(boolean reverse) {
        if (this.entries.isEmpty()) {
            return Collections.emptyList();
        }

        Reference2ObjectSortedMap.FastSortedEntrySet<RenderRegion, List<RenderChunk>> entries =
                this.entries.reference2ObjectEntrySet();

        if (reverse) {
            return () -> new Iterator<Map.Entry<RenderRegion, List<RenderChunk>>>() {
                final ObjectBidirectionalIterator<Reference2ObjectMap.Entry<RenderRegion, List<RenderChunk>>> iterator =
                        entries.fastIterator(entries.last());

                @Override
                public boolean hasNext() {
                    return this.iterator.hasPrevious();
                }

                @Override
                public Map.Entry<RenderRegion, List<RenderChunk>> next() {
                    return this.iterator.previous();
                }
            };
        } else {
            return () -> new Iterator<Map.Entry<RenderRegion, List<RenderChunk>>>() {
                final ObjectBidirectionalIterator<Reference2ObjectMap.Entry<RenderRegion, List<RenderChunk>>> iterator =
                        entries.fastIterator();

                @Override
                public boolean hasNext() {
                    return this.iterator.hasNext();
                }

                @Override
                public Map.Entry<RenderRegion, List<RenderChunk>> next() {
                    return this.iterator.next();
                }
            };
        }
    }

    public void clear() {
        this.entries.clear();
    }

    public void add(RenderChunk render) {
        RenderRegion region = render.getRegion();
        List<RenderChunk> sections = this.entries.get(region);

        if (sections == null) {
            this.entries.put(region, sections = new ObjectArrayList<>());
        }

        sections.add(render);
    }

    public int getCount() {
        return this.entries.values()
                .stream()
                .mapToInt(List::size)
                .sum();
    }
}
