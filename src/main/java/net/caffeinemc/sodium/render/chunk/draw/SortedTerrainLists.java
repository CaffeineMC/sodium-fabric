package net.caffeinemc.sodium.render.chunk.draw;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import net.caffeinemc.sodium.SodiumClientMod;
import net.caffeinemc.sodium.render.chunk.RenderSection;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPassManager;
import net.caffeinemc.sodium.render.chunk.region.RenderRegion;
import net.caffeinemc.sodium.render.chunk.region.RenderRegionManager;
import net.caffeinemc.sodium.render.chunk.state.ChunkPassModel;
import net.caffeinemc.sodium.render.chunk.state.ChunkRenderBounds;
import net.caffeinemc.sodium.render.terrain.quad.properties.ChunkMeshFace;
import net.caffeinemc.sodium.util.collections.BitArray;

public class SortedTerrainLists {
    private static final int REGIONS_ESTIMATE = 32; // idk lol
    private static final int SECTIONS_ESTIMATE = RenderRegion.REGION_SIZE / 2;
    private static final int INITIAL_CACHE_SIZE = 256;
    
    private final RenderRegionManager regionManager;
    private final ChunkRenderPassManager renderPassManager;
    
    public final List<RenderRegion> regions;
    public final List<LongList> uploadedSegments;
    public final List<IntList> sectionCoords;
    public final IntList[] regionIndices;
    public final List<IntList>[] modelPartCounts;
    public final List<LongList>[] modelPartSegments;
    
    // pools to save on many list allocations
    private final Deque<LongList> uploadedSegmentsListPool;
    private final Deque<IntList> sectionCoordsListPool;
    private final Deque<IntList> modelPartCountsListPool;
    private final Deque<LongList> modelPartSegmentsListPool;
    
    private int totalSectionCount;

    @SuppressWarnings("unchecked")
    public SortedTerrainLists(RenderRegionManager regionManager, ChunkRenderPassManager renderPassManager) {
        this.regionManager = regionManager;
        this.renderPassManager = renderPassManager;
        
        int totalPasses = renderPassManager.getRenderPassCount();
    
        this.regions = new ReferenceArrayList<>(REGIONS_ESTIMATE);
        this.uploadedSegments = new ReferenceArrayList<>(REGIONS_ESTIMATE);
        this.sectionCoords = new ReferenceArrayList<>(REGIONS_ESTIMATE);
        this.regionIndices = new IntList[totalPasses];
        this.modelPartCounts = new List[totalPasses];
        this.modelPartSegments = new List[totalPasses];
        
        for (int passId = 0; passId < totalPasses; passId++) {
            this.regionIndices[passId] = new IntArrayList(REGIONS_ESTIMATE);
            this.modelPartCounts[passId] = new ReferenceArrayList<>(REGIONS_ESTIMATE);
            this.modelPartSegments[passId] = new ReferenceArrayList<>(REGIONS_ESTIMATE);
        }
        
        this.uploadedSegmentsListPool = new ArrayDeque<>(INITIAL_CACHE_SIZE);
        this.sectionCoordsListPool = new ArrayDeque<>(INITIAL_CACHE_SIZE);
        this.modelPartCountsListPool = new ArrayDeque<>(INITIAL_CACHE_SIZE);
        this.modelPartSegmentsListPool = new ArrayDeque<>(INITIAL_CACHE_SIZE);
    }
    
    private void reset() {
        this.regions.clear();
    
        for (IntList list : this.regionIndices) {
            list.clear();
        }
    
        // flush everything out to the list caches
        this.uploadedSegmentsListPool.addAll(this.uploadedSegments);
        this.uploadedSegments.clear();
    
        this.sectionCoordsListPool.addAll(this.sectionCoords);
        this.sectionCoords.clear();
        
        for (List<IntList> list : this.modelPartCounts) {
            this.modelPartCountsListPool.addAll(list);
            list.clear();
        }
        
        for (List<LongList> list : this.modelPartSegments) {
            this.modelPartSegmentsListPool.addAll(list);
            list.clear();
        }
    }
    
    private LongList getUploadedSegmentsList() {
        LongList cachedList = this.uploadedSegmentsListPool.pollLast();
        if (cachedList != null) {
            cachedList.clear();
            return cachedList;
        } else {
            return new LongArrayList(SECTIONS_ESTIMATE);
        }
    }
    
    private IntList getSectionCoordsList() {
        IntList cachedList = this.sectionCoordsListPool.pollLast();
        if (cachedList != null) {
            cachedList.clear();
            return cachedList;
        } else {
            return new IntArrayList(SECTIONS_ESTIMATE * 3); // component count for position (x, y, z)
        }
    }
    
    private IntList getModelPartCountsList() {
        IntList cachedList = this.modelPartCountsListPool.pollLast();
        if (cachedList != null) {
            cachedList.clear();
            return cachedList;
        } else {
            return new IntArrayList(SECTIONS_ESTIMATE);
        }
    }
    
    private LongList getModelPartSegmentsList() {
        LongList cachedList = this.modelPartSegmentsListPool.pollLast();
        if (cachedList != null) {
            cachedList.clear();
            return cachedList;
        } else {
            return new LongArrayList(SECTIONS_ESTIMATE * ChunkMeshFace.COUNT);
        }
    }
    
    public void update(List<RenderSection> sortedSections, ChunkCameraContext camera) {
        this.reset();
        
        if (sortedSections.isEmpty()) {
            return;
        }
        
        boolean useBlockFaceCulling = SodiumClientMod.options().performance.useBlockFaceCulling;
        int totalPasses = this.renderPassManager.getRenderPassCount();
        int regionTableSize = this.regionManager.getRegionTableSize();
    
        // lookup tables, indexed by region id
        LongList[] uploadedSegmentsTable = new LongList[regionTableSize];
        IntList[] sectionCoordsTable = new IntList[regionTableSize];
        IntList[][] modelPartCountsTable = new IntList[regionTableSize][totalPasses];
        LongList[][] modelPartSegmentsTable = new LongList[regionTableSize][totalPasses];
        // start with -1 so the first index will become 0 after the increment
        int sequentialRegionIdx = -1;
    
        int totalSectionCount = 0;
        
        for (RenderSection section : sortedSections) {
            boolean sectionAdded = false;
    
            IntList[] regionModelPartCounts = null;
            LongList[] regionModelPartSegments = null;
    
            for (int passId = 0; passId < totalPasses; passId++) {
                // prior checks to avoid any unnecessary allocation
                ChunkPassModel model = section.getData().models[passId];
    
                if (model == null) {
                    continue;
                }
    
                int visibilityBits = model.getVisibilityBits();

                if (useBlockFaceCulling) {
                    visibilityBits &= calculateCameraVisibilityBits(section.getData().bounds, camera);
                }

                if (visibilityBits == 0) {
                    continue;
                }
    
                RenderRegion region = section.getRegion();
                int regionId = region.getId();
    
                // lazily allocate everything here
                
                // add per-section data, and make sure to only add once
                // warning: don't use passId in here
                if (!sectionAdded) {
                    LongList regionUploadedSegmentsList = uploadedSegmentsTable[regionId];
                    IntList regionSectionCoordsList = sectionCoordsTable[regionId];
                    regionModelPartCounts = modelPartCountsTable[regionId];
                    regionModelPartSegments = modelPartSegmentsTable[regionId];
    
                    // if one is null, the region hasn't been processed yet
                    if (regionUploadedSegmentsList == null) {
                        regionUploadedSegmentsList = this.getUploadedSegmentsList();
                        uploadedSegmentsTable[regionId] = regionUploadedSegmentsList;
                        this.uploadedSegments.add(regionUploadedSegmentsList);
                        
                        regionSectionCoordsList = this.getSectionCoordsList();
                        sectionCoordsTable[regionId] = regionSectionCoordsList;
                        this.sectionCoords.add(regionSectionCoordsList);
                        
                        this.regions.add(region);
    
                        sequentialRegionIdx++;
                    }
    
                    regionUploadedSegmentsList.add(section.getUploadedGeometrySegment());
                    
                    regionSectionCoordsList.add(section.getChunkX());
                    regionSectionCoordsList.add(section.getChunkY());
                    regionSectionCoordsList.add(section.getChunkZ());
                    
                    totalSectionCount++;
                    sectionAdded = true;
                }
    
                // add per-section-pass data
                IntList regionPassModelPartCountsList = regionModelPartCounts[passId];
                LongList regionPassModelPartSegmentsList = regionModelPartSegments[passId];
    
                // if one is null, the region + pass combination hasn't been processed yet
                if (regionPassModelPartCountsList == null) {
                    regionPassModelPartCountsList = this.getModelPartCountsList();
                    regionModelPartCounts[passId] = regionPassModelPartCountsList;
                    this.modelPartCounts[passId].add(regionPassModelPartCountsList);
                    
                    regionPassModelPartSegmentsList = this.getModelPartSegmentsList();
                    regionModelPartSegments[passId] = regionPassModelPartSegmentsList;
                    this.modelPartSegments[passId].add(regionPassModelPartSegmentsList);
    
                    this.regionIndices[passId].add(sequentialRegionIdx);
                }
    
                int modelPartCount = 0;
                long[] modelPartSegments = model.getModelPartSegments();
                for (int dirIdx = 0; dirIdx < ChunkMeshFace.COUNT; dirIdx++) {
                    if ((visibilityBits & (1 << dirIdx)) != 0) {
                        regionPassModelPartSegmentsList.add(modelPartSegments[dirIdx]);
                        modelPartCount++;
                    }
                }
                regionPassModelPartCountsList.add(modelPartCount);
            }
        }
        
        for (IntList list : this.regionIndices) {
            IntSet set = new IntOpenHashSet(list);
            if (list.size() != set.size()) {
                System.err.println("problem");
            }
        }
        
        this.totalSectionCount = totalSectionCount;
    }
    
    protected static int calculateCameraVisibilityBits(ChunkRenderBounds bounds, ChunkCameraContext camera) {
        int bits = ChunkMeshFace.UNASSIGNED_BITS;
        
        if (camera.posY > bounds.y1) {
            bits |= ChunkMeshFace.UP_BITS;
        }
        
        if (camera.posY < bounds.y2) {
            bits |= ChunkMeshFace.DOWN_BITS;
        }
        
        if (camera.posX > bounds.x1) {
            bits |= ChunkMeshFace.EAST_BITS;
        }
        
        if (camera.posX < bounds.x2) {
            bits |= ChunkMeshFace.WEST_BITS;
        }
        
        if (camera.posZ > bounds.z1) {
            bits |= ChunkMeshFace.SOUTH_BITS;
        }
        
        if (camera.posZ < bounds.z2) {
            bits |= ChunkMeshFace.NORTH_BITS;
        }
        
        return bits;
    }

    public int getTotalSectionCount() {
        return this.totalSectionCount;
    }

    public boolean isEmpty() {
        return this.totalSectionCount == 0;
    }
    
}
