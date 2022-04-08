package net.caffeinemc.sodium.render.chunk.draw;

import it.unimi.dsi.fastutil.longs.Long2ReferenceLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceSortedMaps;
import it.unimi.dsi.fastutil.objects.*;
import net.caffeinemc.sodium.render.chunk.RenderSection;
import net.caffeinemc.sodium.render.chunk.region.RenderRegion;
import net.caffeinemc.sodium.render.chunk.region.RenderRegionManager;
import net.caffeinemc.sodium.util.IteratorUtils;

import java.util.Iterator;

public class ChunkRenderList {
    private final ReferenceArrayList<RenderSection> sections;
    private final ReferenceArrayList<Bucket> buckets;

    public ChunkRenderList(RenderRegionManager renderRegions, ReferenceArrayList<RenderSection> unsortedSections) {
        var builders = new Long2ReferenceLinkedOpenHashMap<ReferenceArrayList<RenderSection>>();

        var lastListId = Long.MAX_VALUE;
        ReferenceArrayList<RenderSection> lastSortedList = null;

        for (RenderSection section : unsortedSections) {
            var regionId = section.getRegionKey();

            if (lastSortedList == null || lastListId != regionId) {
                lastListId = regionId;
                lastSortedList = builders.get(regionId);

                if (lastSortedList == null) {
                    builders.put(regionId, lastSortedList =
                            new ReferenceArrayList<>(RenderRegion.REGION_SIZE / 4));
                }
            }

            lastSortedList.add(section);
        }

        var buckets = new ReferenceArrayList<Bucket>(builders.size());

        for (var entry : Long2ReferenceSortedMaps.fastIterable(builders)) {
            var regionId = entry.getLongKey();
            var sectionList = entry.getValue();

            var region = renderRegions.getRegion(regionId);

            if (region != null) {
                buckets.add(new Bucket(region, sectionList));
            }
        }

        this.buckets = buckets;
        this.sections = unsortedSections;
    }

    public void add(RenderSection render) {
        this.sections.add(render);
    }

    public int sectionCount() {
        return this.sections.size();
    }

    public int regionCount() {
        return this.buckets.size();
    }

    public Iterator<Bucket> sorted(boolean reverse) {
        return IteratorUtils.reversibleIterator(this.buckets, reverse);
    }

    public Iterable<Bucket> unsorted() {
        return this.buckets;
    }

    public record Bucket(RenderRegion region,
                         ReferenceArrayList<RenderSection> sections) {
        public int size() {
            return this.sections.size();
        }

        public Iterator<RenderSection> sorted(boolean reverse) {
            return IteratorUtils.reversibleIterator(this.sections, reverse);
        }
    }
}
