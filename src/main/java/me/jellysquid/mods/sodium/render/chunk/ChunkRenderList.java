package me.jellysquid.mods.sodium.render.chunk;

import it.unimi.dsi.fastutil.objects.*;
import me.jellysquid.mods.sodium.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.render.chunk.region.RenderRegion;
import net.minecraft.block.entity.BlockEntity;

import java.util.*;

public class ChunkRenderList {
    private final Reference2ObjectLinkedOpenHashMap<RenderRegion, List<Entry>> entries = new Reference2ObjectLinkedOpenHashMap<>();

    private final ObjectList<RenderSection> tickingSections = new ObjectArrayList<>();
    private final ObjectList<BlockEntity> visibleBlockEntities = new ObjectArrayList<>();

    public Iterable<Map.Entry<RenderRegion, List<Entry>>> sortedSectionsByRegion(boolean reverse) {
        if (this.entries.isEmpty()) {
            return Collections.emptyList();
        }

        Reference2ObjectSortedMap.FastSortedEntrySet<RenderRegion, List<Entry>> entries =
                this.entries.reference2ObjectEntrySet();

        if (reverse) {
            return () -> new Iterator<>() {
                final ObjectBidirectionalIterator<Reference2ObjectMap.Entry<RenderRegion, List<Entry>>> iterator =
                        entries.fastIterator(entries.last());

                @Override
                public boolean hasNext() {
                    return this.iterator.hasPrevious();
                }

                @Override
                public Map.Entry<RenderRegion, List<Entry>> next() {
                    return this.iterator.previous();
                }
            };
        } else {
            return () -> new Iterator<>() {
                final ObjectBidirectionalIterator<Reference2ObjectMap.Entry<RenderRegion, List<Entry>>> iterator =
                        entries.fastIterator();

                @Override
                public boolean hasNext() {
                    return this.iterator.hasNext();
                }

                @Override
                public Map.Entry<RenderRegion, List<Entry>> next() {
                    return this.iterator.next();
                }
            };
        }
    }

    public void clear() {
        this.entries.clear();

        this.visibleBlockEntities.clear();
        this.tickingSections.clear();
    }

    public void add(RenderSection render, int visibility) {
        RenderRegion region = render.getRegion();

        List<Entry> sections = this.entries.computeIfAbsent(region, (key) -> new ObjectArrayList<>());
        sections.add(new Entry(render, visibility));

        ChunkRenderData data = render.getData();
        Collection<BlockEntity> blockEntities = data.getBlockEntities();

        if (!blockEntities.isEmpty()) {
            this.visibleBlockEntities.addAll(blockEntities);
        }

        if (render.isTickable()) {
            this.tickingSections.add(render);
        }
    }

    public int getVisibleCount() {
        return this.entries.values()
                .stream()
                .mapToInt(List::size)
                .sum();
    }

    public Collection<BlockEntity> getVisibleBlockEntities() {
        return this.visibleBlockEntities;
    }

    public Iterable<? extends RenderSection> getTickingSections() {
        return this.tickingSections;
    }

    public record Entry(RenderSection section, int visibility) {

    }
}
