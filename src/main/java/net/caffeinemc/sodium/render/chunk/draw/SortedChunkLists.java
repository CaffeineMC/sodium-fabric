package net.caffeinemc.sodium.render.chunk.draw;

import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.caffeinemc.sodium.render.chunk.RenderSection;
import net.caffeinemc.sodium.render.chunk.region.RenderRegion;
import net.caffeinemc.sodium.render.chunk.region.RenderRegionManager;
import net.caffeinemc.sodium.util.IteratorUtils;

import java.util.Iterator;

public class SortedChunkLists {
    private final ReferenceArrayList<RenderSection> sections;
    private final ReferenceArrayList<Bucket> buckets;

    @SuppressWarnings("unchecked")
    public SortedChunkLists(RenderRegionManager regionManager, ReferenceArrayList<RenderSection> sortedSections) {
        ReferenceArrayList<Bucket> bucketList = new ReferenceArrayList<>();
        ReferenceArrayList<RenderSection>[] bucketTable = new ReferenceArrayList[regionManager.getRegionTableSize()];

        for (RenderSection section : sortedSections) {
            var region = section.getRegion();
            var list = bucketTable[region.id];

            if (list == null) {
                list = new ReferenceArrayList<>(RenderRegion.REGION_SIZE / 4);

                bucketList.add(new Bucket(region, list));
                bucketTable[region.id] = list;
            }

            list.add(section);
        }

        this.buckets = bucketList;
        this.sections = sortedSections;
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

    public boolean isEmpty() {
        return this.sections.isEmpty();
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
