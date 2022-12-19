package net.caffeinemc.sodium.render.chunk;

import net.caffeinemc.sodium.render.chunk.draw.ChunkCameraContext;
import net.caffeinemc.sodium.util.collections.BitArray;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.HeightLimitView;

public class SectionTree {
    public static final int NULL_INDEX = 0xFFFFFFFF;
    
    public final ChunkCameraContext camera;
    
    public final int chunkLoadDistance;
    public final int maxDepth; // inclusive
    
    public final int sectionHeightMin;
    public final int sectionHeightMax;
    public final int sectionHeight;
    public final int sectionWidth;
    public final int sectionWidthSquared;
    public final int sectionWidthMask;
    public final int sectionWidthOffset;
    public final int sectionHeightOffset;
    public final int sectionTableSize;
    
    public final int idxYShift;
    public final int idxZShift;
    public final int idxYMask;
    public final int idxZMask;
    public final int idxXMask;
    
    public final int[] nodeHeights;
    public final int[] nodeArrayOffsets;
    
    // TODO: slowly take fields out of this class and move them into parallel arrays here
    public final RenderSection[] sections;
    public final byte[] sectionFlagData;
    public final BitArray sectionExistenceBits;
    
    // these do not include the first level of nodes, which are the individual sections.
    // unsigned shorts support a max depth of 5, signed short support a max depth of 4
    public final short[] nodeLoadedSections;
    
    private int totalLoadedSections;
    
    public SectionTree(int maxDepth, int chunkLoadDistance, HeightLimitView heightLimitView, ChunkCameraContext camera) {
        this.sectionHeightMin = heightLimitView.getBottomSectionCoord();
        this.sectionHeightMax = heightLimitView.getTopSectionCoord();
        
        this.chunkLoadDistance = chunkLoadDistance;
    
        // Make the width (in sections) a power-of-two, so we can exploit bit-wise math when computing indices
        this.sectionWidth = MathHelper.smallestEncompassingPowerOfTwo(chunkLoadDistance * 2 + 1);
        this.sectionWidthOffset = chunkLoadDistance;
        this.sectionWidthSquared = this.sectionWidth * this.sectionWidth;
        
        this.sectionWidthMask = this.sectionWidth - 1;
        this.idxZShift = Integer.numberOfTrailingZeros(this.sectionWidth);
        this.idxYShift = this.idxZShift * 2;
        this.idxYMask = -(1 << this.idxYShift);
        this.idxXMask = this.sectionWidthMask;
        this.idxZMask = this.sectionWidthMask << this.idxZShift;
    
        this.maxDepth = Math.min(maxDepth, Integer.SIZE - Integer.numberOfLeadingZeros(this.sectionWidth) - 1);
        
        this.sectionHeight = heightLimitView.countVerticalSections();
        this.sectionHeightOffset = -heightLimitView.getBottomSectionCoord();
        
        this.sectionTableSize = this.sectionWidthSquared * this.sectionHeight;
        
        this.sections = new RenderSection[this.sectionTableSize];
        this.sectionFlagData = new byte[this.sectionTableSize];
        this.sectionExistenceBits = new BitArray(this.sectionTableSize);
        
        this.nodeArrayOffsets = new int[this.maxDepth];
        this.nodeHeights = new int[this.maxDepth];
        int totalOffset = 0;
        int nodeWidth = this.sectionWidth;
        int currentHeight = this.sectionHeight;
        int heightRound = 0;
        
        for (int i = 0; i < this.maxDepth; i++) {
            // To get an accurate number that's able to store all the elements we need, we need to make sure to round up
            // the divisor to make sure that nodes which may be caught on the border are included.
            heightRound |= (0b1 & currentHeight);
            currentHeight >>= 1;
            int nodeHeight = currentHeight + heightRound;
    
            nodeWidth >>= 1;
            
            this.nodeHeights[i] = nodeHeight;
            this.nodeArrayOffsets[i] = totalOffset;
            
            totalOffset += (nodeHeight * nodeWidth * nodeWidth);
        }
        
        // don't include the first level of nodes, because those are already stored in the sectionExistenceBits
        this.nodeLoadedSections = new short[totalOffset];
        
        this.camera = camera;
    }
    
    public int getSectionIdx(int x, int y, int z) {
        if (this.isSectionInLoadBounds(x, y, z)) {
                return this.getSectionIdxUnchecked(x, y, z);
        } else {
            return NULL_INDEX;
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
        int tableY = y + this.sectionHeightOffset;
        int tableZ = z & this.sectionWidthMask;
        int tableX = x & this.sectionWidthMask;
        return (tableY << this.idxYShift)
               | (tableZ << this.idxZShift)
               | tableX;
    }
    
    public int getSectionIdx(RenderSection section) {
        return this.getSectionIdx(section.getSectionX(), section.getSectionY(), section.getSectionZ());
    }
    
    protected int getNodeIdx(int depth, int x, int y, int z) {
        int nodeWidthShift = this.idxZShift >> depth;
        int nodeArrayOffset = this.nodeArrayOffsets[depth - 1];
        
        int tableY = (y + this.sectionHeightOffset) >> depth;
        int tableZ = (z & this.sectionWidthMask) >> depth;
        int tableX = (x & this.sectionWidthMask) >> depth;
        int innerIdx = (tableY << nodeWidthShift * 2)
                       | (tableZ << nodeWidthShift)
                       | tableX;
    
        return nodeArrayOffset + innerIdx;
    }
    
    public RenderSection add(int x, int y, int z) {
        this.totalLoadedSections++;
        
        RenderSection section = new RenderSection(x, y, z);
    
        // TODO: make this not unchecked?
        //  maybe do checks outside of the add and keep them in the tracker until they can be added
        int sectionIdx = this.getSectionIdxUnchecked(x, y, z);
        
        this.sections[sectionIdx] = section;
        boolean prevExists = this.sectionExistenceBits.getAndSet(sectionIdx);
        
        if (!prevExists) {
            // skip bottom level of nodes
            for (int depth = 1; depth <= this.maxDepth; depth++) {
                int nodeIdx = this.getNodeIdx(depth, x, y, z);
                this.nodeLoadedSections[nodeIdx]++;
            }
        }
    
        return section;
    }
    
    public RenderSection remove(int x, int y, int z) {
        this.totalLoadedSections--;
        
        int sectionIdx = this.getSectionIdxUnchecked(x, y, z);
        
        RenderSection section = this.sections[sectionIdx];
        this.sections[sectionIdx] = null;

        boolean prevExists = this.sectionExistenceBits.getAndUnset(sectionIdx);

        // skip bottom level of nodes
        if (prevExists) {
            for (int depth = 1; depth <= this.maxDepth; depth++) {
                int nodeIdx = this.getNodeIdx(depth, x, y, z);
                this.nodeLoadedSections[nodeIdx]--;
            }
        }
        
        return section;
    }
    
    public RenderSection getSection(int x, int y, int z) {
        return this.getSection(this.getSectionIdx(x, y, z));
    }
    
    public RenderSection getSection(int sectionIdx) {
        if (sectionIdx == NULL_INDEX) {
            return null;
        }
        
        return this.sections[sectionIdx];
    }
    
    public int getSectionTableSize() {
        return this.sectionTableSize;
    }
    
    public int getLoadedSections() {
        return this.totalLoadedSections;
    }
}
