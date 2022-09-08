package net.caffeinemc.sodium.render.chunk.cull;

import net.caffeinemc.sodium.render.chunk.RenderSection;
import net.caffeinemc.sodium.render.chunk.draw.ChunkCameraContext;
import net.caffeinemc.sodium.util.collections.BitArray;
import net.minecraft.world.HeightLimitView;

public class SectionTree {
    public static final int OUT_OF_BOUNDS_INDEX = 0xFFFFFFFF;
    
    protected final ChunkCameraContext camera;
    
    protected final int chunkLoadDistance;
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
//    protected final Long2ReferenceMap<RenderSection> backupSections;
    
    // these do not include the first level of nodes, which are the individual sections.
    // unsigned shorts support a max depth of 5, signed short support a max depth of 4
    protected final short[] loadedSectionsPerNode;
    protected final int[] nodeArrayOffsets;
    
    private int loadedSections;
    
    public SectionTree(int maxDepth, int chunkLoadDistance, HeightLimitView heightLimitView, ChunkCameraContext camera) {
        this.sectionHeightMin = heightLimitView.getBottomSectionCoord();
        this.sectionHeightMax = heightLimitView.getTopSectionCoord() - 1;
        
        this.chunkLoadDistance = chunkLoadDistance;
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
        
        this.nodeArrayOffsets = new int[maxDepth];
        int totalOffset = 0;
        double currentSize = this.sectionTableSize;
        
        for (int i = 0; i < maxDepth; i++) {
            // To get an accurate number that's able to store all the elements we need, we need to make sure to round up
            // the divisor to make sure that nodes which may be caught on the border are included.
            this.nodeArrayOffsets[i] = totalOffset;
            currentSize /= 8.0f;
            totalOffset += (int) Math.ceil(currentSize);
        }
        
        // don't include the first level of nodes, because those are already stored in the sectionExistenceBits
        this.loadedSectionsPerNode = new short[totalOffset];
        
        
//        this.backupSections = new Long2ReferenceOpenHashMap<>(this.sectionTableSize / 8);
        
        this.camera = camera;
    }
    
    public int getSectionIdx(int x, int y, int z) {
        if (this.isSectionInLoadBounds(x, y, z)) {
                return this.getSectionIdxUnchecked(x, y, z);
        } else {
            return OUT_OF_BOUNDS_INDEX;
        }
    }
    
    public boolean isSectionInLoadBounds(int x, int y, int z) {
        int offsetY = y + this.sectionHeightOffset;
        int offsetZ = this.camera.getSectionZ() - z + this.sectionWidthOffset;
        int offsetX = this.camera.getSectionX() - x + this.sectionWidthOffset;
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
    
    protected int getNodeIdx(int depth, int sectionIdx) {
        return this.getNodeDepthOffset(depth) + (sectionIdx >> (3 * depth));
    }
    
    protected int getNodeDepthOffset(int depth) {
        return this.nodeArrayOffsets[depth - 1];
    }
    
    public RenderSection add(int x, int y, int z) {
        this.loadedSections++;
        
        RenderSection section = new RenderSection(x, y, z);
    
        // TODO: make this not unchecked?
        int sectionIdx = this.getSectionIdxUnchecked(x, y, z);
    
//        if (sectionIdx != OUT_OF_BOUNDS_INDEX) {
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
            
            // skip bottom level of nodes
            for (int depth = 1; depth <= this.maxDepth; depth++) {
                int nodeIdx = this.getNodeIdx(depth, sectionIdx);
                this.loadedSectionsPerNode[nodeIdx]++;
            }
//        } else {
//            this.backupSections.put(ChunkSectionPos.asLong(x, y, z), section);
//        }
    
        return section;
    }
    
    public RenderSection remove(int x, int y, int z) {
        this.loadedSections--;
        
        int sectionIdx = this.getSectionIdxUnchecked(x, y, z);
        
        
//        if (sectionIdx != OUT_OF_BOUNDS_INDEX) {
            RenderSection section = this.sections[sectionIdx];
            this.sections[sectionIdx] = null;
    
            this.sectionExistenceBits.unset(sectionIdx);
    
            // skip bottom level of nodes
            for (int depth = 1; depth <= this.maxDepth; depth++) {
                int nodeIdx = this.getNodeIdx(depth, sectionIdx);
                this.loadedSectionsPerNode[nodeIdx]--;
            }
            
            return section;
//        } else {
//            return this.backupSections.remove(ChunkSectionPos.asLong(x, y, z));
//        }
    
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
