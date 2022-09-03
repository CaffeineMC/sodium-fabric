package net.caffeinemc.sodium.render.chunk.occlusion;

import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import net.caffeinemc.sodium.render.chunk.RenderSection;
import net.caffeinemc.sodium.render.chunk.draw.ChunkCameraContext;
import net.caffeinemc.sodium.util.collections.BitArray;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.HeightLimitView;

public class SectionTree {
    public static final int OUT_OF_BOUNDS_INDEX = 0xFFFFFFFF;
    
    protected final ChunkCameraContext camera;
    
    protected final int chunkViewDistance;
    protected final int maxDepth; // inclusive
    
    protected final int sectionHeightMin;
    protected final int sectionHeightMax;
    protected final int sectionWidth;
    protected final int sectionHeight;
    protected final int sectionWidthSquared;
    protected final int sectionWidthOffset;
    protected final int sectionHeightOffset;
    protected final int sectionTableSize;
    
    protected final RenderSection[] sections;
    protected final BitArray sectionExistenceBits;
    // used so we can deal with out-of-bounds sections in the same way as vanilla
    // TODO: periodically move these into the array when they're in range
    //  maybe remove?
    protected final Long2ReferenceMap<RenderSection> backupSections;
//    protected final int[] loadedSectionsPerNode;
    
    private int loadedSections;
    
    public SectionTree(int maxDepth, int chunkLoadDistance, HeightLimitView heightLimitView, ChunkCameraContext camera) {
        this.sectionHeightMin = heightLimitView.getBottomSectionCoord();
        this.sectionHeightMax = heightLimitView.getTopSectionCoord() - 1;
        
        this.chunkViewDistance = chunkLoadDistance;
        this.maxDepth = maxDepth;
    
//        // Make the diameter a power-of-two, so we can exploit bit-wise math when computing indices
//        this.diameter = MathHelper.smallestEncompassingPowerOfTwo(loadDistance * 2 + 1);
        this.sectionWidth = chunkLoadDistance * 2 + 1;
        this.sectionWidthOffset = chunkLoadDistance;
        this.sectionWidthSquared = this.sectionWidth * this.sectionWidth;
        
        this.sectionHeight = heightLimitView.countVerticalSections();
        this.sectionHeightOffset = -heightLimitView.getBottomSectionCoord();
        
        this.sectionTableSize = this.sectionWidthSquared * this.sectionHeight;
        
        this.sections = new RenderSection[this.sectionTableSize];
        this.sectionExistenceBits = new BitArray(this.sectionTableSize);
        this.backupSections = new Long2ReferenceOpenHashMap<>(this.sectionTableSize / 8);
        
        this.camera = camera;
    }
    
//    public int getSectionIdx(int x, int y, int z) {
//        int tableY = y + this.sectionHeightOffset;
//        int tableZ = z - this.originSectionZ + this.sectionWidthOffset;
//        int tableX = x - this.originSectionX + this.sectionWidthOffset;
//        if (tableY < 0 ||
//            tableY >= this.sectionHeight ||
//            tableZ < 0 ||
//            tableZ >= this.sectionWidth ||
//            tableX < 0 ||
//            tableX >= this.sectionWidth) {
//            return ABSENT_VALUE;
//        } else {
//            return tableY * this.sectionWidthSquared
//                   + tableZ * this.sectionWidth
//                   + tableX;
//        }
//    }
    
//    public int getSectionIdx(int x, int y, int z) {
//        int offsetY = y + this.sectionHeightOffset;
//        int offsetZ = z + this.sectionWidthOffset;
//        int offsetX = x + this.sectionWidthOffset;
//        int originZ = offsetZ - this.getOriginSectionZ();
//        int originX = offsetX - this.getOriginSectionX();
//        if (offsetY < 0 ||
//            offsetY >= this.sectionHeight ||
//            originZ < 0 ||
//            originZ >= this.sectionWidth ||
//            originX < 0 ||
//            originX >= this.sectionWidth) {
//            return ABSENT_VALUE;
//        } else {
//            int tableY = Math.floorMod(offsetY, this.sectionHeight);
//            int tableZ = Math.floorMod(offsetZ, this.sectionWidth);
//            int tableX = Math.floorMod(offsetX, this.sectionWidth);
//            return tableY * this.sectionWidthSquared
//                   + tableZ * this.sectionWidth
//                   + tableX;
//        }
//    }
    
    public int getSectionIdx(int x, int y, int z) {
        if (this.isSectionInBounds(x, y, z)) {
                int tableY = Math.floorMod(y, this.sectionHeight);
                int tableZ = Math.floorMod(z, this.sectionWidth);
                int tableX = Math.floorMod(x, this.sectionWidth);
                return tableY * this.sectionWidthSquared
                       + tableZ * this.sectionWidth
                       + tableX;
        } else {
            return OUT_OF_BOUNDS_INDEX;
        }
    }
    
    private boolean isSectionInBounds(int x, int y, int z) {
        int offsetY = y + this.sectionHeightOffset;
        int offsetZ = z + this.sectionWidthOffset - this.camera.getSectionX();
        int offsetX = x + this.sectionWidthOffset - this.camera.getSectionZ();
        return offsetY >= 0 &&
               offsetY < this.sectionHeight &&
               offsetZ >= 0 &&
               offsetZ < this.sectionWidth &&
               offsetX >= 0 &&
               offsetX < this.sectionWidth;
    }
    
    public int getSectionIdxUnchecked(int x, int y, int z) {
        int tableY = Math.floorMod(y, this.sectionHeight);
        int tableZ = Math.floorMod(z, this.sectionWidth);
        int tableX = Math.floorMod(x, this.sectionWidth);
        return tableY * this.sectionWidthSquared
               + tableZ * this.sectionWidth
               + tableX;
    }
    
    public int getSectionIdx(RenderSection section) {
        return this.getSectionIdx(section.getSectionX(), section.getSectionY(), section.getSectionZ());
    }
    
//    private int getNodeIdx(int x, int y, int z, int depth) {
//        int yFromOrigin = this.originSectionY - y + this.sectionHeightOffset;
//        int zFromOrigin = this.originSectionZ - z + this.sectionWidthOffset;
//        int xFromOrigin = this.originSectionX - x + this.sectionWidthOffset;
//        int depthSectionWidth = this.sectionWidth >> depth;
//        return this.getNodeDepthOffset(depth)
//               + (yFromOrigin >> depth) * depthSectionWidth * depthSectionWidth
//               + (zFromOrigin >> depth) * depthSectionWidth
//               + (xFromOrigin >> depth);
//    }
    
    public int getNodeDepthOffset(int depth) {
        // To get an accurate number that's able to store all the elements we need, we need to make sure to "round up"
        // after shifting to make sure that nodes larger than the smallest which may be caught on the border are
        // included.
        //
        // We can know that a node with a specific depth is "aligned" and doesn't need to be rounded up based on if the
        // size being bit-shifted doesn't remove any 1 bits.
        int roundedDepths = Math.max(depth - Integer.numberOfTrailingZeros(this.sectionTableSize), 0);
        return (this.sectionTableSize * 2) - (this.sectionTableSize >> depth) + roundedDepths;
    }
    
    public int getNodeDepthOffset(int startDepth, int depth) {
        int roundedDepths = Math.max(depth - startDepth - Integer.numberOfTrailingZeros(this.sectionTableSize >> startDepth), 0);
        return (this.sectionTableSize * 2) - (this.sectionTableSize >> depth) + roundedDepths;
    }
    
    public RenderSection add(int x, int y, int z) {
        this.loadedSections++;
        
        RenderSection section = new RenderSection(x, y, z);
    
        int sectionIdx = this.getSectionIdxUnchecked(x, y, z);
    
        if (sectionIdx != OUT_OF_BOUNDS_INDEX) {
//            RenderSection existing = this.sections[sectionIdx];
            
//            if (existing != null) {
//                this.backupSections.put(
//                        ChunkSectionPos.asLong(
//                                existing.getSectionX(),
//                                existing.getSectionY(),
//                                existing.getSectionZ()
//                        ),
//                        existing
//                );
//            }
            
            this.sections[sectionIdx] = section;
            this.sectionExistenceBits.set(sectionIdx);
        } else {
            this.backupSections.put(ChunkSectionPos.asLong(x, y, z), section);
        }
    
        return section;
    }
    
    public RenderSection remove(int x, int y, int z) {
        this.loadedSections--;
        
        int sectionIdx = this.getSectionIdxUnchecked(x, y, z);
        
        
        if (sectionIdx != OUT_OF_BOUNDS_INDEX) {
            RenderSection section = this.sections[sectionIdx];
            this.sections[sectionIdx] = null;
    
            this.sectionExistenceBits.unset(sectionIdx);
            
            return section;
        } else {
            return this.backupSections.remove(ChunkSectionPos.asLong(x, y, z));
        }
    
    }
    
    public RenderSection getSection(int x, int y, int z) {
        return this.getSection(this.getSectionIdx(x, y, z));
    }
    
    public RenderSection getSection(int sectionIdx) {
        if (sectionIdx == OUT_OF_BOUNDS_INDEX) {
            return null;
        }
        
        return this.sections[sectionIdx];
    }
    
    public int getSectionTableSize() {
        return this.sectionTableSize;
    }
    
    public int getLoadedSections() {
        return this.loadedSections;
    }
}
