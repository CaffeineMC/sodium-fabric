package net.caffeinemc.sodium.render.chunk.occlusion;

import net.caffeinemc.sodium.render.chunk.RenderSection;
import net.caffeinemc.sodium.util.collections.BitArray;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.HeightLimitView;

public class SectionTree {
    public static final int ABSENT_VALUE = 0xFFFFFFFF;
    
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
    protected final RenderSection[] overwrittenSections; // used so we can unload/remove sections in the same order as vanilla
    protected final BitArray sectionExistenceBits; // TODO: add space for different node depths, update accordingly
    
    private int loadedSections;
    
    private int originSectionX;
    private int originSectionY;
    private int originSectionZ;
    
    public SectionTree(int maxDepth, int chunkViewDistance, HeightLimitView heightLimitView) {
        this.sectionHeightMin = heightLimitView.getBottomSectionCoord();
        this.sectionHeightMax = heightLimitView.getTopSectionCoord() - 1;
        
        this.chunkViewDistance = chunkViewDistance;
        this.maxDepth = maxDepth;
        
        // why are these like this?
        this.sectionWidth = chunkViewDistance * 2 + 3;
        this.sectionWidthOffset = chunkViewDistance + 1;
        
        this.sectionWidthSquared = this.sectionWidth * this.sectionWidth;
        this.sectionHeight = heightLimitView.countVerticalSections();
        this.sectionHeightOffset = -heightLimitView.getBottomSectionCoord();
        this.sectionTableSize = this.sectionWidth * this.sectionWidth * this.sectionHeight;
        
        this.sections = new RenderSection[this.sectionTableSize];
        this.overwrittenSections = new RenderSection[this.sectionTableSize];
        this.sectionExistenceBits = new BitArray(this.sectionTableSize);
    }
    
    public int getSectionIdx(int x, int y, int z) {
        int tableY = y + this.sectionHeightOffset;
        int tableZ = z - this.originSectionZ + this.sectionWidthOffset;
        int tableX = x - this.originSectionX + this.sectionWidthOffset;
        if (tableY < 0 ||
            tableY >= this.sectionHeight ||
            tableZ < 0 ||
            tableZ >= this.sectionWidth ||
            tableX < 0 ||
            tableX >= this.sectionWidth) {
            return ABSENT_VALUE;
        } else {
            return tableY * this.sectionWidthSquared
                   + tableZ * this.sectionWidth
                   + tableX;
        }
    }
    
    private int getNodeIdx(int x, int y, int z, int depth) {
        int yFromOrigin = this.originSectionY - y + this.sectionHeightOffset;
        int zFromOrigin = this.originSectionZ - z + this.sectionWidthOffset;
        int xFromOrigin = this.originSectionX - x + this.sectionWidthOffset;
        int depthSectionWidth = this.sectionWidth >> depth;
        return this.getNodeDepthOffset(depth)
               + (yFromOrigin >> depth) * depthSectionWidth * depthSectionWidth
               + (zFromOrigin >> depth) * depthSectionWidth
               + (xFromOrigin >> depth);
    }
    
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
    
    public int getSectionIdx(RenderSection section) {
        return this.getSectionIdx(section.getSectionX(), section.getSectionY(), section.getSectionZ());
    }
    
    public void setOrigin(BlockPos origin) {
        this.originSectionY = MathHelper.clamp(
                ChunkSectionPos.getSectionCoord(origin.getY()),
                this.sectionHeightMin,
                this.sectionHeightMax
        );
        
        this.originSectionX = ChunkSectionPos.getSectionCoord(origin.getX());
        this.originSectionZ = ChunkSectionPos.getSectionCoord(origin.getZ());
    }
    
    public RenderSection add(int x, int y, int z) {
        this.loadedSections++;
        
        RenderSection section = new RenderSection(x, y, z);
        int sectionIdx = this.getSectionIdx(x, y, z);
    
        RenderSection existing = this.sections[sectionIdx];
        if (existing != null) {
            this.overwrittenSections[sectionIdx] = existing;
        }
        
        this.sections[sectionIdx] = section;
        this.sectionExistenceBits.set(sectionIdx);
        
        return section;
    }
    
    public RenderSection remove(int x, int y, int z) {
        this.loadedSections--;
        
        int sectionIdx = this.getSectionIdx(x, y, z);
        
        if (sectionIdx == ABSENT_VALUE) {
            throw new IllegalArgumentException("Section IDX absent on remove");
        }
        
        RenderSection section = this.overwrittenSections[sectionIdx];
        
        if (section != null) {
            this.overwrittenSections[sectionIdx] = null;
        } else {
            section = this.sections[sectionIdx];
            this.sections[sectionIdx] = null;
        }
        
        this.sectionExistenceBits.unset(sectionIdx);
        
        return section;
    }
    
    public RenderSection getSection(int x, int y, int z) {
        return this.getSection(this.getSectionIdx(x, y, z));
    }
    
    public RenderSection getSection(int sectionIdx) {
        if (sectionIdx == ABSENT_VALUE) {
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
    
    public int getChunkViewDistance() {
        return this.chunkViewDistance;
    }
    
    public int getSectionWidth() {
        return this.sectionWidth;
    }
    
    public int getSectionHeight() {
        return this.sectionHeight;
    }
    
    public int getOriginSectionX() {
        return this.originSectionX;
    }
    
    public int getOriginSectionY() {
        return this.originSectionY;
    }
    
    public int getOriginSectionZ() {
        return this.originSectionZ;
    }
    
}
