package net.caffeinemc.sodium.render.chunk.draw;

import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import java.util.Iterator;
import net.caffeinemc.sodium.render.chunk.RenderSection;
import net.caffeinemc.sodium.render.chunk.region.RenderRegion;
import net.caffeinemc.sodium.render.chunk.region.RenderRegionManager;
import net.caffeinemc.sodium.util.IteratorUtils;

public class SortedChunkLists {
    private final ReferenceArrayList<RegionBucket> regionBuckets;
    private final int sectionCount;

    public SortedChunkLists(ReferenceArrayList<RenderSection> sortedSections, RenderRegionManager regionManager) {
        ReferenceArrayList<RegionBucket> sortedRegionBuckets = new ReferenceArrayList<>();
        // table with efficient lookups for creation, not sorted
        RegionBucket[] bucketTable = new RegionBucket[regionManager.getRegionTableSize()];
        
        for (RenderSection section : sortedSections) {
            RenderRegion region = section.getRegion();
            RegionBucket bucket = bucketTable[region.id];

            if (bucket == null) {
                bucket = new RegionBucket(region);
                sortedRegionBuckets.add(bucket);
                bucketTable[region.id] = bucket;
            }
    
            bucket.addSection(section);
        }

        this.regionBuckets = sortedRegionBuckets;
        this.sectionCount = sortedSections.size();
    }

    public int getSectionCount() {
        return this.sectionCount;
    }

    public int getRegionCount() {
        return this.regionBuckets.size();
    }

    public Iterator<RegionBucket> sortedRegionBuckets(boolean reverse) {
        return IteratorUtils.reversibleIterator(this.regionBuckets, reverse);
    }

    public Iterable<RegionBucket> unsortedRegionBuckets() {
        return this.regionBuckets;
    }

    public boolean isEmpty() {
        return this.sectionCount == 0;
    }
    
    public static class RegionBucket {
        private final RenderRegion region;
        private final ReferenceArrayList<RenderSection> sections;
        
        public RegionBucket(RenderRegion region) {
            this.region = region;
            // estimate that the region will have about 1/4 of non-empty sections
            this.sections = new ReferenceArrayList<>(RenderRegion.REGION_SIZE / 4);
        }
        
        public int getSectionCount() {
            return this.sections.size();
        }
    
        public RenderRegion getRegion() {
            return this.region;
        }
    
        public Iterator<RenderSection> sortedSections(boolean reverse) {
            return IteratorUtils.reversibleIterator(this.sections, reverse);
        }
        
        public ReferenceArrayList<RenderSection> unsortedSections() {
            return this.sections;
        }
        
        private void addSection(RenderSection section) {
            this.sections.add(section);
        }
    }
}
