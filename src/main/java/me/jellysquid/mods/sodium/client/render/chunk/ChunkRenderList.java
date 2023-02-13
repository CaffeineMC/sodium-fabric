package me.jellysquid.mods.sodium.client.render.chunk;

import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.longs.Long2ReferenceLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceSortedMap;
import it.unimi.dsi.fastutil.objects.*;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ChunkRenderList {
    private final Long2ReferenceLinkedOpenHashMap<List<RenderSection>> entries = new Long2ReferenceLinkedOpenHashMap<>();

    public Iterable<Long2ReferenceMap.Entry<List<RenderSection>>> sorted(boolean reverse) {
        if (this.entries.isEmpty()) {
            return Collections.emptyList();
        }

        Long2ReferenceSortedMap.FastSortedEntrySet<List<RenderSection>> entries =
                this.entries.long2ReferenceEntrySet();

        if (reverse) {
            return () -> new Iterator<>() {
                final ObjectBidirectionalIterator<Long2ReferenceMap.Entry<List<RenderSection>>> iterator =
                        entries.fastIterator(entries.last());

                @Override
                public boolean hasNext() {
                    return this.iterator.hasPrevious();
                }

                @Override
                public Long2ReferenceMap.Entry<List<RenderSection>> next() {
                    return this.iterator.previous();
                }
            };
        } else {
            return () -> new Iterator<>() {
                final ObjectBidirectionalIterator<Long2ReferenceMap.Entry<List<RenderSection>>> iterator =
                        entries.fastIterator();

                @Override
                public boolean hasNext() {
                    return this.iterator.hasNext();
                }

                @Override
                public Long2ReferenceMap.Entry<List<RenderSection>> next() {
                    return this.iterator.next();
                }
            };
        }
    }

    public void clear() {
        this.entries.clear();
    }

    public void add(RenderSection render) {
        List<RenderSection> sections = this.entries.get(render.getRegionId());

        if (sections == null) {
            this.entries.put(render.getRegionId(), sections = new ObjectArrayList<>(RenderRegion.REGION_SIZE));
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
