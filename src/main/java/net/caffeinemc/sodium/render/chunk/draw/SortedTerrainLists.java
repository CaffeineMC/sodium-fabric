package net.caffeinemc.sodium.render.chunk.draw;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import java.util.ArrayDeque;
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
        
        this.totalSectionCount = 0;
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
//        StringBuilder debugStringBuilder = new StringBuilder("start generate lists");
        
        this.reset();
        
        if (sortedSections.isEmpty()) {
            return;
        }
        
        boolean useBlockFaceCulling = SodiumClientMod.options().performance.useBlockFaceCulling;
        int totalPasses = this.renderPassManager.getRenderPassCount();
        int regionTableSize = this.regionManager.getRegionTableSize();
    
        // lookup tables indexed by region id
        LongList[] uploadedSegmentsTable = new LongList[regionTableSize];
        IntList[] sectionCoordsTable = new IntList[regionTableSize];
        int[] sequentialRegionIdxTable = new int[regionTableSize];
        
        int sequentialRegionIdx = 0;
        
        // lookup tables indexed by region id and pass id
        IntList[][] modelPartCountsTable = new IntList[regionTableSize][totalPasses];
        LongList[][] modelPartSegmentsTable = new LongList[regionTableSize][totalPasses];
        
        int totalSectionCount = 0;
        
        for (RenderSection section : sortedSections) {
//            debugStringBuilder.append("\n--------------------------------------------sectionId ")
//                              .append(section.getId())
//                              .append(", regionId ")
//                              .append(section.getRegion().getId())
//                              .append("\npasses: ");
            boolean sectionAdded = false;
    
            IntList[] regionModelPartCounts = null;
            LongList[] regionModelPartSegments = null;
    
            for (int passId = 0; passId < totalPasses; passId++) {
//                debugStringBuilder.append(passId).append("    , "); // we leave space that can be replaced with nsrp
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
    
//                debugStringBuilder.setCharAt(debugStringBuilder.length() - 6, 'n');
                RenderRegion region = section.getRegion();
                int regionId = region.getId();
    
                // lazily allocate everything here
                
                // add per-section data, and make sure to only add once
                // warning: don't use passId in here
                if (!sectionAdded) {
//                    debugStringBuilder.setCharAt(debugStringBuilder.length() - 5, 's');
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
                        
                        // set, then increment
//                        debugStringBuilder.setCharAt(debugStringBuilder.length() - 4, Integer.toString(sequentialRegionIdx).charAt(0));
                        sequentialRegionIdxTable[regionId] = sequentialRegionIdx++;
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
                    
//                    debugStringBuilder.setCharAt(debugStringBuilder.length() - 3, Integer.toString(sequentialRegionIdxTable[regionId]).charAt(0));
                    this.regionIndices[passId].add(sequentialRegionIdxTable[regionId]);
                }
                
                long[] modelPartSegments = model.getModelPartSegments();
    
                regionPassModelPartCountsList.add(Integer.bitCount(visibilityBits));
                
                // faster way to iterate through indices of set bits in an integer
                // warning: don't use visibilityBits after this function, because the function is destructive
                while (visibilityBits != 0) {
                    int dirIdx = Integer.numberOfTrailingZeros(visibilityBits);
                    regionPassModelPartSegmentsList.add(modelPartSegments[dirIdx]);
                    visibilityBits &= visibilityBits - 1;
                }
            }
    
            // remove the final comma
//            debugStringBuilder.setLength(debugStringBuilder.length() - 2);
        }
        
//        System.out.println(debugStringBuilder);
        
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
