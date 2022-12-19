package net.caffeinemc.sodium.render.chunk.draw;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceList;
import net.caffeinemc.sodium.SodiumClientMod;
import net.caffeinemc.sodium.render.chunk.RenderSection;
import net.caffeinemc.sodium.render.chunk.SortedSectionLists;
import net.caffeinemc.sodium.render.chunk.SectionTree;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPass;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPassManager;
import net.caffeinemc.sodium.render.chunk.region.RenderRegion;
import net.caffeinemc.sodium.render.chunk.region.RenderRegionManager;
import net.caffeinemc.sodium.render.chunk.state.SectionPassModel;
import net.caffeinemc.sodium.render.chunk.state.ChunkRenderBounds;
import net.caffeinemc.sodium.render.chunk.state.SectionRenderData;
import net.caffeinemc.sodium.render.terrain.quad.properties.ChunkMeshFace;
import net.minecraft.util.math.Vec3d;

public class SortedTerrainLists {
    private static final int REGIONS_ESTIMATE = 128;
    private static final int SECTIONS_PER_REGION_ESTIMATE = RenderRegion.REGION_SIZE / 2;
    private static final int INITIAL_CACHE_SIZE = 256;
    
    private final RenderRegionManager regionManager;
    private final ChunkRenderPassManager renderPassManager;
    private final SortedSectionLists sortedSectionLists;
    private final ChunkCameraContext camera;
    
    public final ReferenceList<RenderRegion> regions;
    public final IntList[] regionIndices;
    public final ReferenceList<LongList> uploadedSegments;
    public final ReferenceList<IntList> sectionCoords;
    public final ReferenceList<IntList>[] sectionIndices;
    public final ReferenceList<IntList>[] modelPartCounts;
    public final ReferenceList<LongList>[] modelPartSegments;
    
    // pools to save on many list allocations
    private final ReferenceArrayList<LongList> uploadedSegmentsListPool;
    private final ReferenceArrayList<IntList> sectionCoordsListPool;
    private final ReferenceArrayList<IntList> sectionIndicesListPool;
    private final ReferenceArrayList<IntList> modelPartCountsListPool;
    private final ReferenceArrayList<LongList> modelPartSegmentsListPool;
    
    private int finalSectionCount;

    @SuppressWarnings("unchecked")
    public SortedTerrainLists(
            RenderRegionManager regionManager,
            ChunkRenderPassManager renderPassManager,
            SortedSectionLists sortedSectionLists,
            ChunkCameraContext camera
    ) {
        this.regionManager = regionManager;
        this.renderPassManager = renderPassManager;
        this.sortedSectionLists = sortedSectionLists;
        this.camera = camera;
        
        int totalPasses = renderPassManager.getRenderPassCount();
    
        this.regions = new ReferenceArrayList<>(REGIONS_ESTIMATE);
        this.uploadedSegments = new ReferenceArrayList<>(REGIONS_ESTIMATE);
        this.sectionCoords = new ReferenceArrayList<>(REGIONS_ESTIMATE);
        this.regionIndices = new IntList[totalPasses];
        this.sectionIndices = new ReferenceList[totalPasses];
        this.modelPartCounts = new ReferenceList[totalPasses];
        this.modelPartSegments = new ReferenceList[totalPasses];
        
        for (int passId = 0; passId < totalPasses; passId++) {
            this.regionIndices[passId] = new IntArrayList(REGIONS_ESTIMATE);
            this.sectionIndices[passId] = new ReferenceArrayList<>(REGIONS_ESTIMATE);
            this.modelPartCounts[passId] = new ReferenceArrayList<>(REGIONS_ESTIMATE);
            this.modelPartSegments[passId] = new ReferenceArrayList<>(REGIONS_ESTIMATE);
        }
        
        this.uploadedSegmentsListPool = new ReferenceArrayList<>(INITIAL_CACHE_SIZE);
        this.sectionCoordsListPool = new ReferenceArrayList<>(INITIAL_CACHE_SIZE);
        this.sectionIndicesListPool = new ReferenceArrayList<>(INITIAL_CACHE_SIZE);
        this.modelPartCountsListPool = new ReferenceArrayList<>(INITIAL_CACHE_SIZE);
        this.modelPartSegmentsListPool = new ReferenceArrayList<>(INITIAL_CACHE_SIZE);
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
    
        for (ReferenceList<IntList> list : this.sectionIndices) {
            this.sectionIndicesListPool.addAll(list);
            list.clear();
        }
        
        for (ReferenceList<IntList> list : this.modelPartCounts) {
            this.modelPartCountsListPool.addAll(list);
            list.clear();
        }
        
        for (ReferenceList<LongList> list : this.modelPartSegments) {
            this.modelPartSegmentsListPool.addAll(list);
            list.clear();
        }
        
        this.finalSectionCount = 0;
    }
    
    private LongList getUploadedSegmentsList() {
        if (this.uploadedSegmentsListPool.isEmpty()) {
            return new LongArrayList(SECTIONS_PER_REGION_ESTIMATE);
        } else {
            LongList cachedList = this.uploadedSegmentsListPool.pop();
            cachedList.clear();
            return cachedList;
        }
    }
    
    private IntList getSectionCoordsList() {
        if (this.sectionCoordsListPool.isEmpty()) {
            return new IntArrayList(SECTIONS_PER_REGION_ESTIMATE * 3); // component count for position (x, y, z)
        } else {
            IntList cachedList = this.sectionCoordsListPool.pop();
            cachedList.clear();
            return cachedList;
        }
    }
    
    private IntList getSectionIndicesList() {
        if (this.sectionIndicesListPool.isEmpty()) {
            return new IntArrayList(SECTIONS_PER_REGION_ESTIMATE);
        } else {
            IntList cachedList = this.sectionIndicesListPool.pop();
            cachedList.clear();
            return cachedList;
        }
    }
    
    private IntList getModelPartCountsList() {
        if (this.modelPartCountsListPool.isEmpty()) {
            return new IntArrayList(SECTIONS_PER_REGION_ESTIMATE);
        } else {
            IntList cachedList = this.modelPartCountsListPool.pop();
            cachedList.clear();
            return cachedList;
        }
    }
    
    private LongList getModelPartSegmentsList() {
        if (this.modelPartSegmentsListPool.isEmpty()) {
            return new LongArrayList(SECTIONS_PER_REGION_ESTIMATE * ChunkMeshFace.COUNT);
        } else {
            LongList cachedList = this.modelPartSegmentsListPool.pop();
            cachedList.clear();
            return cachedList;
        }
    }
    
    public void update() {
        this.reset();
        
        if (this.sortedSectionLists.terrainSectionCount == 0) {
            return;
        }
        
        Vec3d cameraPos = this.camera.getPos();
        boolean useBlockFaceCulling = SodiumClientMod.options().performance.useBlockFaceCulling;
        ChunkRenderPass[] chunkRenderPasses = this.renderPassManager.getAllRenderPasses();
        int totalPasses = chunkRenderPasses.length;
        int regionTableSize = this.regionManager.getRegionTableSize();
    
        // lookup tables indexed by region id
        LongList[] uploadedSegmentsTable = new LongList[regionTableSize];
        IntList[] sectionCoordsTable = new IntList[regionTableSize];
        int[] sequentialRegionIdxTable = new int[regionTableSize];
        
        // lookup tables indexed by region id and pass id
        IntList[][] modelPartCountsTable = new IntList[regionTableSize][totalPasses];
        LongList[][] modelPartSegmentsTable = new LongList[regionTableSize][totalPasses];
        IntList[][] sectionIndicesTable = new IntList[regionTableSize][totalPasses];
        
        // index counters
        int sequentialRegionIdx = 0;
        
        int totalSectionCount = 0;
        
        for (RenderSection section : this.sortedSectionLists.getTerrainSections()) {
            boolean sectionAdded = false;
    
            int sequentialSectionIdx = 0;
            IntList[] regionModelPartCounts = null;
            LongList[] regionModelPartSegments = null;
            IntList[] regionSectionIndices = null;
    
            for (int passId = 0; passId < totalPasses; passId++) {
                SectionRenderData sectionRenderData = section.getData();
                SectionPassModel model = sectionRenderData.models[passId];
                
                // skip if the section has no models for the pass
                if (model == null) {
                    continue;
                }
    
                int visibilityBits = model.getVisibilityBits();

                if (useBlockFaceCulling) {
                    visibilityBits &= calculateCameraVisibilityBits(sectionRenderData.bounds, cameraPos);
                }

                // skip if the section has no *visible* models for the pass
                if (visibilityBits == 0) {
                    continue;
                }
                
                RenderRegion region = section.getRegion();
                int regionId = region.getId();
    
                // lazily allocate everything here
                
                // add per-section data, and make sure to only add once
                // warning: don't use passId in here
                if (!sectionAdded) {
                    LongList regionUploadedSegments = uploadedSegmentsTable[regionId];
                    IntList regionSectionCoords = sectionCoordsTable[regionId];
                    regionModelPartCounts = modelPartCountsTable[regionId];
                    regionModelPartSegments = modelPartSegmentsTable[regionId];
                    regionSectionIndices = sectionIndicesTable[regionId];
    
                    // if one is null, the region hasn't been processed yet
                    if (regionUploadedSegments == null) {
                        regionUploadedSegments = this.getUploadedSegmentsList();
                        uploadedSegmentsTable[regionId] = regionUploadedSegments;
                        this.uploadedSegments.add(regionUploadedSegments);
                        
                        regionSectionCoords = this.getSectionCoordsList();
                        sectionCoordsTable[regionId] = regionSectionCoords;
                        this.sectionCoords.add(regionSectionCoords);
                        
                        this.regions.add(region);
                        
                        // set, then increment
                        sequentialRegionIdxTable[regionId] = sequentialRegionIdx++;
                    }
    
                    // get size before adding, avoiding unnecessary subtraction
                    sequentialSectionIdx = regionUploadedSegments.size();
                    regionUploadedSegments.add(section.getUploadedGeometrySegment());
                    
                    regionSectionCoords.add(section.getSectionX());
                    regionSectionCoords.add(section.getSectionY());
                    regionSectionCoords.add(section.getSectionZ());
                    
                    totalSectionCount++;
                    sectionAdded = true;
                }
    
                // add per-section-pass data
                IntList regionPassModelPartCounts = regionModelPartCounts[passId];
                LongList regionPassModelPartSegments = regionModelPartSegments[passId];
                IntList regionPassSectionIndices = regionSectionIndices[passId];
    
                // if one is null, the region + pass combination hasn't been processed yet
                if (regionPassModelPartCounts == null) {
                    regionPassModelPartCounts = this.getModelPartCountsList();
                    regionModelPartCounts[passId] = regionPassModelPartCounts;
                    this.modelPartCounts[passId].add(regionPassModelPartCounts);
    
                    regionPassModelPartSegments = this.getModelPartSegmentsList();
                    regionModelPartSegments[passId] = regionPassModelPartSegments;
                    this.modelPartSegments[passId].add(regionPassModelPartSegments);
    
                    regionPassSectionIndices = this.getSectionIndicesList();
                    regionSectionIndices[passId] = regionPassSectionIndices;
                    this.sectionIndices[passId].add(regionPassSectionIndices);
                    
                    this.regionIndices[passId].add(sequentialRegionIdxTable[regionId]);
                }
    
                regionPassSectionIndices.add(sequentialSectionIdx);
                regionPassModelPartCounts.add(Integer.bitCount(visibilityBits));
                
                // We want to make sure the direction order is the same, whether the pass reverses
                // the iteration or not. It's faster to do that here, because it'll allow the
                // iteration to effectively prefetch these values, allowing either a fully
                // sequential forwards or backwards iteration.
                //
                // These functions also employ faster ways to iterate through indices of set bits
                // in an integer.
                // warning: don't use visibilityBits after these functions, they are destructive
                boolean reverseOrder = chunkRenderPasses[passId].isTranslucent();
                long[] modelPartSegments = model.getModelPartSegments();
                
                if (reverseOrder) {
                    while (visibilityBits != 0) {
                        int dirIdx = (Integer.SIZE - 1) - Integer.numberOfLeadingZeros(visibilityBits);
                        regionPassModelPartSegments.add(modelPartSegments[dirIdx]);
                        visibilityBits ^= 1 << dirIdx;
                    }
                } else {
                    while (visibilityBits != 0) {
                        int dirIdx = Integer.numberOfTrailingZeros(visibilityBits);
                        regionPassModelPartSegments.add(modelPartSegments[dirIdx]);
                        visibilityBits &= visibilityBits - 1;
                    }
                }
            }
        }
        
        this.finalSectionCount = totalSectionCount;
    }
    
    protected static int calculateCameraVisibilityBits(ChunkRenderBounds bounds, Vec3d cameraPos) {
        int bits = ChunkMeshFace.UNASSIGNED_BITS;
        
        if (cameraPos.getY() > bounds.y1) {
            bits |= ChunkMeshFace.UP_BITS;
        }
        
        if (cameraPos.getY() < bounds.y2) {
            bits |= ChunkMeshFace.DOWN_BITS;
        }
        
        if (cameraPos.getX() > bounds.x1) {
            bits |= ChunkMeshFace.EAST_BITS;
        }
        
        if (cameraPos.getX() < bounds.x2) {
            bits |= ChunkMeshFace.WEST_BITS;
        }
        
        if (cameraPos.getZ() > bounds.z1) {
            bits |= ChunkMeshFace.SOUTH_BITS;
        }
        
        if (cameraPos.getZ() < bounds.z2) {
            bits |= ChunkMeshFace.NORTH_BITS;
        }
        
        return bits;
    }

    public int getFinalSectionCount() {
        return this.finalSectionCount;
    }

    public boolean isEmpty() {
        return this.finalSectionCount == 0;
    }
    
}
