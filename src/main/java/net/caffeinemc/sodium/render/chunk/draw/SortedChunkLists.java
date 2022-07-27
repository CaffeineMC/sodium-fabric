package net.caffeinemc.sodium.render.chunk.draw;

import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import java.util.Iterator;
import java.util.List;
import net.caffeinemc.sodium.render.chunk.RenderSection;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPass;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPassManager;
import net.caffeinemc.sodium.render.chunk.region.RenderRegion;
import net.caffeinemc.sodium.render.chunk.region.RenderRegionManager;
import net.caffeinemc.sodium.render.chunk.state.ChunkRenderBounds;
import net.caffeinemc.sodium.render.terrain.quad.properties.ChunkMeshFace;
import net.caffeinemc.sodium.util.IteratorUtils;

public class SortedChunkLists {
    private final List<RegionBucket>[] regionBucketsPerPass;
    private final int sectionCount;

    public SortedChunkLists(List<RenderSection> sortedSections, RenderRegionManager regionManager, ChunkRenderPassManager renderPassManager) {
        ChunkRenderPass[] chunkRenderPasses = renderPassManager.getAllRenderPasses();
        int totalPasses = chunkRenderPasses.length;
        
        @SuppressWarnings("unchecked")
        List<RegionBucket>[] sortedBucketsPerPass = new ReferenceArrayList[totalPasses];
        
        // table with efficient lookups for creation, not sorted
        RegionBucket[] bucketTable = new RegionBucket[regionManager.getRegionTableSize()];
        
        for (RenderSection section : sortedSections) {
            RenderRegion region = section.getRegion();
            int regionId = region.getId();
            RegionBucket bucket = bucketTable[regionId];

            if (bucket == null) {
                bucket = new RegionBucket(region);
                bucketTable[regionId] = bucket;
    
                int cameraVisibilityBits = useBlockFaceCulling ? calculateVisibilityFlags(section.getData().bounds, camera) : ChunkMeshFace.ALL_BITS;
    
                for (int i = 0; i < totalPasses; i++) {
                    // lazily allocate list, do checks prior to creation
                }
            }
    
            bucket.addSection(section);
        }

        this.regionBucketsPerPass = sortedBucketsPerPass;
        this.sectionCount = sortedSections.size();
    }
    
    protected static int calculateVisibilityFlags(ChunkRenderBounds bounds, ChunkCameraContext camera) {
        int flags = ChunkMeshFace.UNASSIGNED_BITS;
        
        if (camera.posY > bounds.y1) {
            flags |= ChunkMeshFace.UP_BITS;
        }
        
        if (camera.posY < bounds.y2) {
            flags |= ChunkMeshFace.DOWN_BITS;
        }
        
        if (camera.posX > bounds.x1) {
            flags |= ChunkMeshFace.EAST_BITS;
        }
        
        if (camera.posX < bounds.x2) {
            flags |= ChunkMeshFace.WEST_BITS;
        }
        
        if (camera.posZ > bounds.z1) {
            flags |= ChunkMeshFace.SOUTH_BITS;
        }
        
        if (camera.posZ < bounds.z2) {
            flags |= ChunkMeshFace.NORTH_BITS;
        }
        
        return flags;
    }

    public int getSectionCount() {
        return this.sectionCount;
    }

    public Iterator<RegionBucket> sortedRegionBuckets(int passId, boolean reverse) {
        return IteratorUtils.reversibleIterator(this.regionBucketsPerPass[passId], reverse);
    }

    public Iterable<RegionBucket> unsortedRegionBuckets() {
        return this.regionBuckets;
    }

    public boolean isEmpty() {
        return this.sectionCount == 0;
    }
    
    public static class RegionBucket {
        private final RenderRegion region;
        
        private final IntList sectionVisibility
        private final LongList sectionUploadedSegments;
        private final LongList sectionModelPartSegments;
        
        public RegionBucket(RenderRegion region) {
            this.region = region;
            int sectionsEstimate = RenderRegion.REGION_SIZE / 2;
            this.sectionUploadedSegments = new LongArrayList(sectionsEstimate);
            this.sectionModelPartSegments = new LongArrayList(sectionsEstimate * ChunkMeshFace.COUNT);
        }
        
        public int getSectionCount() {
            return this.sectionUploadedSegments.size();
        }
    
        public RenderRegion getRegion() {
            return this.region;
        }
        
        private void addSection(RenderSection section) {
            this.sections.add(section);
        }
    }
}
