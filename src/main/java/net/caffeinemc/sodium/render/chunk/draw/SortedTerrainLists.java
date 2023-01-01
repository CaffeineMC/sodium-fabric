package net.caffeinemc.sodium.render.chunk.draw;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.caffeinemc.sodium.SodiumClientMod;
import net.caffeinemc.sodium.render.chunk.RenderSection;
import net.caffeinemc.sodium.render.chunk.SectionTree;
import net.caffeinemc.sodium.render.chunk.SortedSectionLists;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPass;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPassManager;
import net.caffeinemc.sodium.render.chunk.region.RenderRegion;
import net.caffeinemc.sodium.render.chunk.region.RenderRegionManager;
import net.caffeinemc.sodium.render.chunk.state.ChunkRenderBounds;
import net.caffeinemc.sodium.render.chunk.state.SectionPassModel;
import net.caffeinemc.sodium.render.chunk.state.SectionRenderData;
import net.caffeinemc.sodium.render.terrain.quad.properties.ChunkMeshFace;
import net.caffeinemc.sodium.util.Pool;
import net.minecraft.util.math.Vec3d;

public class SortedTerrainLists {
    private static final int REGIONS_ESTIMATE = 128;
    
    private final RenderRegionManager regionManager;
    private final ChunkRenderPassManager renderPassManager;
    private final SectionTree sectionTree;
    private final SortedSectionLists sortedSectionLists;
    private final ChunkCameraContext camera;
    
    public final ReferenceArrayList<RenderRegion> regions;
    public final ReferenceArrayList<LongArrayList> uploadedSegments;
    public final ReferenceArrayList<IntArrayList> sectionCoords;
    
    public final BuiltPass[] builtPasses;
    
    // pools to save on many list allocations
    private final Pool<LongArrayList> uploadedSegmentsListPool;
    private final Pool<IntArrayList> sectionCoordsListPool;
    private final Pool<BuiltPass.BuiltRegion> builtRegionPool;
    
    private int finalSectionCount;

    public SortedTerrainLists(
            RenderRegionManager regionManager,
            ChunkRenderPassManager renderPassManager,
            SectionTree sectionTree,
            SortedSectionLists sortedSectionLists,
            ChunkCameraContext camera
    ) {
        this.regionManager = regionManager;
        this.renderPassManager = renderPassManager;
        this.sectionTree = sectionTree;
        this.sortedSectionLists = sortedSectionLists;
        this.camera = camera;
    
        int passCount = renderPassManager.getRenderPassCount();
        this.builtPasses = new BuiltPass[passCount];
    
        for (int i = 0; i < this.builtPasses.length; i++) {
            this.builtPasses[i] = new BuiltPass();
        }
    
        this.regions = new ReferenceArrayList<>(REGIONS_ESTIMATE);
        this.uploadedSegments = new ReferenceArrayList<>(REGIONS_ESTIMATE);
        this.sectionCoords = new ReferenceArrayList<>(REGIONS_ESTIMATE);
        
        this.uploadedSegmentsListPool = new Pool<>(
                REGIONS_ESTIMATE,
                () -> new LongArrayList(RenderRegion.REGION_SIZE)
        );
        this.sectionCoordsListPool = new Pool<>(
                REGIONS_ESTIMATE,
                () -> new IntArrayList(RenderRegion.REGION_SIZE * 3)
        );
        this.builtRegionPool = new Pool<>(
                REGIONS_ESTIMATE,
                BuiltPass.BuiltRegion::new
        );
    }
    
    private void reset() {
        this.regions.clear();
    
        var builtPasses = this.builtPasses;
        // manual for loops done for performance in hot code
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < builtPasses.length; i++) {
            builtPasses[i].clear();
        }
        
        // combine clearing uploaded segments and section coords because they will be the same size
        var uploadedSegments = this.uploadedSegments;
        var sectionCoords = this.sectionCoords;
        for (int i = 0; i < uploadedSegments.size(); i++) {
            uploadedSegments.get(i).clear();
            sectionCoords.get(i).clear();
        }
        
        // flush everything out to the list caches
        this.uploadedSegmentsListPool.release(this.uploadedSegments);
        this.sectionCoordsListPool.release(this.sectionCoords);
        
        this.finalSectionCount = 0;
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
        var uploadedSegmentsTable = new LongArrayList[regionTableSize];
        var sectionCoordsTable = new IntArrayList[regionTableSize];
        var sequentialRegionIdxTable = new int[regionTableSize];

        var builtRegionTable = new BuiltPass.BuiltRegion[regionTableSize][totalPasses];

        // index counters
        int sequentialRegionIdx = 0;
        
        int totalSectionCount = 0;
        
        for (int i = 0; i < this.sortedSectionLists.terrainSectionCount; i++) {
            int sectionIdx = this.sortedSectionLists.terrainSectionIdxs[i];
            RenderSection section = this.sectionTree.getSection(sectionIdx);
            
            boolean sectionAdded = false;
            int sequentialSectionIdx = 0;
            
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
                    var regionUploadedSegments = uploadedSegmentsTable[regionId];
                    var regionSectionCoords = sectionCoordsTable[regionId];

                    // if one is null, the region hasn't been processed yet
                    if (regionUploadedSegments == null) {
                        regionUploadedSegments = this.uploadedSegmentsListPool.acquire();
                        uploadedSegmentsTable[regionId] = regionUploadedSegments;
                        this.uploadedSegments.add(regionUploadedSegments);

                        regionSectionCoords = this.sectionCoordsListPool.acquire();
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

                var builtRegion = builtRegionTable[regionId][passId];
    
                if (builtRegion == null) {
                    // the region + pass combination hasn't been processed yet
                    builtRegion = this.builtRegionPool.acquire();
                    builtRegionTable[regionId][passId] = builtRegion;

                    var pass = this.builtPasses[passId];
                    pass.regionIndices.add(sequentialRegionIdxTable[regionId]);
                    pass.builtRegions.add(builtRegion);
                }

                // add per-section-pass data
                builtRegion.sectionIndices.add(sequentialSectionIdx);
                builtRegion.modelPartCounts.add(Integer.bitCount(visibilityBits));
                
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
                        builtRegion.modelPartSegments.add(modelPartSegments[dirIdx]);
                        visibilityBits ^= 1 << dirIdx;
                    }
                } else {
                    while (visibilityBits != 0) {
                        int dirIdx = Integer.numberOfTrailingZeros(visibilityBits);
                        builtRegion.modelPartSegments.add(modelPartSegments[dirIdx]);
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
    
    public class BuiltPass {
        public final IntArrayList regionIndices;
        public final ReferenceArrayList<BuiltRegion> builtRegions;
    
        public BuiltPass() {
            this.regionIndices = new IntArrayList(REGIONS_ESTIMATE);
            this.builtRegions = new ReferenceArrayList<>(REGIONS_ESTIMATE);
        }
    
        public void clear() {
            this.regionIndices.clear();
    
            // manual for loop done for performance in hot code
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < this.builtRegions.size(); i++) {
                this.builtRegions.get(i).clear();
            }
            SortedTerrainLists.this.builtRegionPool.release(this.builtRegions);
        }
        
        public static class BuiltRegion {
            public final IntArrayList sectionIndices;
            public final IntArrayList modelPartCounts;
            
            public final LongArrayList modelPartSegments;
            
            public BuiltRegion() {
                this.sectionIndices = new IntArrayList(RenderRegion.REGION_SIZE);
                this.modelPartCounts = new IntArrayList(RenderRegion.REGION_SIZE);
                this.modelPartSegments = new LongArrayList(RenderRegion.REGION_SIZE * ChunkMeshFace.COUNT);
            }
            
            public void clear() {
                this.sectionIndices.clear();
                this.modelPartCounts.clear();
                this.modelPartSegments.clear();
            }
        }
    }
}
