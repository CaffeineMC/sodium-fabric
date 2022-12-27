package net.caffeinemc.sodium.render.chunk.cull;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.BitSet;
import net.caffeinemc.gfx.util.misc.MathUtil;
import net.caffeinemc.sodium.interop.vanilla.math.frustum.Frustum;
import net.caffeinemc.sodium.render.chunk.SectionTree;
import net.caffeinemc.sodium.render.chunk.SortedSectionLists;
import net.caffeinemc.sodium.util.DirectionUtil;
import net.caffeinemc.sodium.util.collections.BitArray;
import net.minecraft.client.render.chunk.ChunkOcclusionData;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class SectionCuller {
    
    private static final long VIS_DATA_MASK = ~(-1L << DirectionUtil.COUNT);
    private static final int XP = 1 << DirectionUtil.X_PLUS;
    private static final int XN = 1 << DirectionUtil.X_MIN;
    private static final int ZP = 1 << DirectionUtil.Z_PLUS;
    private static final int ZN = 1 << DirectionUtil.Z_MIN;
    
    private final SectionTree sectionTree;
    private final SortedSectionLists sortedSectionLists;
    private final int chunkViewDistance;
    private final double squaredFogDistance;
    
    private final byte[] sectionDirVisibilityData;
    private final BitArray sectionVisibilityBitsPass1;
    private final BitArray sectionVisibilityBitsPass2;
    
    /**
     * Temporary array that stores all sections (including air sections and sections that haven't been loaded yet) in a
     * bfs-sorted array.
     */
    private final int[] sortedSections;
    
    /**
     * An array of shorts which represents the concatenation of the allowed and visible traversal directions.
     * allowed: The directions that the bfs is still allowed to check
     * visible: The outgoing directions out of the chunk
     */
    private final short[] sectionTraversalData;
    
    private final IntList[] fallbackSectionLists;
    
    public SectionCuller(SectionTree sectionTree, SortedSectionLists sortedSectionLists, int chunkViewDistance) {
        this.sectionTree = sectionTree;
        this.sortedSectionLists = sortedSectionLists;
        this.chunkViewDistance = chunkViewDistance;
        this.squaredFogDistance = MathHelper.square((chunkViewDistance + 1) * 16.0);
        
        this.fallbackSectionLists = new IntList[chunkViewDistance * 2 + 1];
        // the first list will have a known size of 1 always
        this.fallbackSectionLists[0] = new IntArrayList(1);
        for (int i = 1; i < this.fallbackSectionLists.length; i++) {
            this.fallbackSectionLists[i] = new IntArrayList();
        }
        
        this.sectionDirVisibilityData = new byte[sectionTree.getSectionTableSize() * DirectionUtil.COUNT];
        this.sectionVisibilityBitsPass1 = new BitArray(sectionTree.getSectionTableSize());
        this.sectionVisibilityBitsPass2 = new BitArray(sectionTree.getSectionTableSize());
        
        this.sectionTraversalData = new short[sectionTree.getSectionTableSize()];
        this.sortedSections = new int[sectionTree.getSectionTableSize()];
    }
    
    public void calculateVisibleSections(
            Frustum frustum,
            boolean useOcclusionCulling
    ) {
        this.sectionVisibilityBitsPass1.fill(false);
        this.sectionVisibilityBitsPass2.fill(false);
    
        if (this.sectionTree.getLoadedSections() != 0) {
            // Start with corner section of the fog distance.
            // To do this, we have to reverse the function to check if a chunk is in bounds by doing pythagorean's
            // theorem, then doing some math.
            double cameraX = this.sectionTree.camera.getPosX();
            double cameraZ = this.sectionTree.camera.getPosZ();
            double sectionCenterDistX = MathUtil.floorMod(cameraX, 16.0) - 8.0;
            double sectionCenterDistZ = MathUtil.floorMod(cameraZ, 16.0) - 8.0;
            double distX = Math.sqrt(this.squaredFogDistance - (sectionCenterDistZ * sectionCenterDistZ));
            double distZ = Math.sqrt(this.squaredFogDistance - (sectionCenterDistX * sectionCenterDistX));
            
            int sectionZStart = ChunkSectionPos.getSectionCoord(cameraZ - distZ - 8.0);
            int sectionXStart = ChunkSectionPos.getSectionCoord(cameraX - distX - 8.0);
            int sectionZEnd = ChunkSectionPos.getSectionCoord(cameraZ + distZ + 8.0);
            int sectionXEnd = ChunkSectionPos.getSectionCoord(cameraX + distX + 8.0);
            
            this.frustumCull(
                    frustum,
                    sectionZStart,
                    sectionXStart,
                    sectionZEnd,
                    sectionXEnd
            );

            this.fogCull(
                    sectionZStart,
                    sectionXStart,
                    sectionZEnd,
                    sectionXEnd
            );
            
            // still need to do this to maintain ordering between sections, even if useOcclusionCulling is false
            this.occlusionCullAndFillLists(
                    useOcclusionCulling,
                    sectionZStart,
                    sectionXStart,
                    sectionZEnd,
                    sectionXEnd
            );
        }
    }
    
    // inlining the locals makes it harder to read
    @SuppressWarnings("UnnecessaryLocalVariable")
    private void frustumCull(
            Frustum frustum,
            final int sectionZStart,
            final int sectionXStart,
            final int sectionZEnd,
            final int sectionXEnd
    ) {
        int nodeSectionLength = 1 << this.sectionTree.maxDepth;
    
        int yIdxIncrement = nodeSectionLength * this.sectionTree.sectionWidthSquared;
        int zIdxIncrement = nodeSectionLength * this.sectionTree.sectionWidth;
        int xIdxIncrement = nodeSectionLength;
    
        // Z and X table indices *are* restricted to the table.
        final int tableZStart = sectionZStart & this.sectionTree.sectionWidthMask;
        final int tableXStart = sectionXStart & this.sectionTree.sectionWidthMask;
        
        final int zIdxStart = tableZStart * this.sectionTree.sectionWidth;
        final int xIdxStart = tableXStart;
        final int zIdxWrap = this.sectionTree.sectionWidthSquared;
        final int xIdxWrap = this.sectionTree.sectionWidth;
        
        int sectionZSplit = Math.min(sectionZStart + this.sectionTree.sectionWidth - tableZStart, sectionZEnd);
        int sectionXSplit = Math.min(sectionXStart + this.sectionTree.sectionWidth - tableXStart, sectionXEnd);
        
        for (int sectionY = this.sectionTree.sectionHeightMin, yIdxOffset = 0; sectionY < this.sectionTree.sectionHeightMax; sectionY += nodeSectionLength, yIdxOffset += yIdxIncrement) {
            int sectionZ = sectionZStart;
            int sectionZMax = sectionZSplit;
            int zIdxOffset = zIdxStart;
            while (true) {
                if (zIdxOffset >= zIdxWrap && sectionZMax != sectionZEnd) {
                    zIdxOffset = 0;
                    sectionZ = sectionZMax;
                    sectionZMax = sectionZEnd;
                }
    
                if (sectionZ >= sectionZEnd) {
                    break;
                }
    
                int sectionX = sectionXStart;
                int sectionXMax = sectionXSplit;
                int xIdxOffset = xIdxStart;
                while (true) {
                    if (xIdxOffset >= xIdxWrap && sectionXMax != sectionXEnd) {
                        xIdxOffset = 0;
                        sectionX = sectionXMax;
                        sectionXMax = sectionXEnd;
                    }
    
                    if (sectionX >= sectionXEnd) {
                        break;
                    }
                    
                    this.checkNode(
                           frustum,
                           sectionY,
                           sectionZ,
                           sectionX,
                           sectionZMax,
                           sectionXMax,
                           this.sectionTree.maxDepth,
                           yIdxOffset + zIdxOffset + xIdxOffset,
                           Frustum.BLANK_RESULT
                    );
                    
                    sectionX += nodeSectionLength;
                    xIdxOffset += xIdxIncrement;
                }
                
                sectionZ += nodeSectionLength;
                zIdxOffset += zIdxIncrement;
            }
        }
    }
    
    @SuppressWarnings("SuspiciousNameCombination")
    private void checkNode(
            Frustum frustum,
            int sectionY,
            int sectionZ,
            int sectionX,
            int sectionZMax,
            int sectionXMax,
            int depth,
            int sectionIdx,
            int parentTestResult
    ) {
        final int nodeSectionLength = 1 << depth;
    
        final int sectionYEnd = Math.min(sectionY + nodeSectionLength, this.sectionTree.sectionHeightMax);
        final int sectionZEnd = Math.min(sectionZ + nodeSectionLength, sectionZMax);
        final int sectionXEnd = Math.min(sectionX + nodeSectionLength, sectionXMax);
    
        float minY = (float) ChunkSectionPos.getBlockCoord(sectionY);
        float minZ = (float) ChunkSectionPos.getBlockCoord(sectionZ);
        float minX = (float) ChunkSectionPos.getBlockCoord(sectionX);
        float maxY = (float) ChunkSectionPos.getBlockCoord(sectionYEnd);
        float maxZ = (float) ChunkSectionPos.getBlockCoord(sectionZEnd);
        float maxX = (float) ChunkSectionPos.getBlockCoord(sectionXEnd);
    
        int frustumTestResult = frustum.intersectBox(minX, minY, minZ, maxX, maxY, maxZ, parentTestResult);
    
        if (frustumTestResult != Frustum.OUTSIDE) {
            if (frustumTestResult == Frustum.INSIDE) {
                for (int newSectionY = sectionY, yIdxOffset = 0; newSectionY < sectionYEnd; newSectionY++, yIdxOffset += this.sectionTree.sectionWidthSquared) {
                    for (int newSectionZ = sectionZ, zIdxOffset = 0; newSectionZ < sectionZEnd; newSectionZ++, zIdxOffset += this.sectionTree.sectionWidth) {
                        this.sectionVisibilityBitsPass1.copy(
                                this.sectionTree.sectionExistenceBits,
                                sectionIdx + yIdxOffset + zIdxOffset,
                                sectionIdx + yIdxOffset + zIdxOffset + sectionXEnd - sectionX
                        );
                    }
                }
            } else {
                int childDepth = depth - 1;
                int childSectionLength = nodeSectionLength >> 1;

                int yIdxIncrement = childSectionLength * this.sectionTree.sectionWidthSquared;
                int zIdxIncrement = childSectionLength * this.sectionTree.sectionWidth;
                
                for (int newSectionY = sectionY, yIdxOffset = 0; newSectionY < sectionYEnd; newSectionY += childSectionLength, yIdxOffset += yIdxIncrement) {
                    for (int newSectionZ = sectionZ, zIdxOffset = 0; newSectionZ < sectionZEnd; newSectionZ += childSectionLength, zIdxOffset += zIdxIncrement) {
                        for (int newSectionX = sectionX, xIdxOffset = 0; newSectionX < sectionXEnd; newSectionX += childSectionLength, xIdxOffset += childSectionLength) {
                            
                            int newSectionIdx = sectionIdx + yIdxOffset + zIdxOffset + xIdxOffset;
                            // check should get moved outside of loop
                            if (childDepth == 0) {
                                this.checkSection(
                                        frustum,
                                        newSectionY,
                                        newSectionZ,
                                        newSectionX,
                                        newSectionIdx,
                                        frustumTestResult
                                );
                            } else {
                                this.checkNode(
                                        frustum,
                                        newSectionY,
                                        newSectionZ,
                                        newSectionX,
                                        sectionZMax,
                                        sectionXMax,
                                        childDepth,
                                        newSectionIdx,
                                        frustumTestResult
                                );
                            }
                        }
                    }
                }
            }
        }
    }
    
    private void checkSection(
            Frustum frustum,
            int sectionY,
            int sectionZ,
            int sectionX,
            int sectionIdx,
            int parentTestResult
    ) {
        // skip if the section is empty
        if (!this.sectionTree.sectionExistenceBits.get(sectionIdx)) {
            return;
        }
    
        float minY = (float) ChunkSectionPos.getBlockCoord(sectionY);
        float minZ = (float) ChunkSectionPos.getBlockCoord(sectionZ);
        float minX = (float) ChunkSectionPos.getBlockCoord(sectionX);
        float maxY = minY + 16.0f;
        float maxZ = minZ + 16.0f;
        float maxX = minX + 16.0f;
    
        if (frustum.containsBox(minX, minY, minZ, maxX, maxY, maxZ, parentTestResult)) {
            // we already tested that it does exist, so we can unconditionally set
            this.sectionVisibilityBitsPass1.set(sectionIdx);
        }
    }

    // always use a cylindrical cull for fog.
    // we don't want to cull above and below the player for various reasons.
    //
    // inlining the locals makes it harder to read
    @SuppressWarnings({"UnnecessaryLocalVariable", "SuspiciousNameCombination"})
    private void fogCull(
            final int sectionZStart,
            final int sectionXStart,
            final int sectionZEnd,
            final int sectionXEnd
    ) {
        int zIdxIncrement = this.sectionTree.sectionWidth;
        int xIdxIncrement = 1;
    
        // Table indices *are* restricted to the table.
        final int tableZStart = sectionZStart & this.sectionTree.sectionWidthMask;
        final int tableXStart = sectionXStart & this.sectionTree.sectionWidthMask;
        
        final int zIdxStart = tableZStart * this.sectionTree.sectionWidth;
        final int xIdxStart = tableXStart;
        final int zIdxWrap = this.sectionTree.sectionWidthSquared;
        final int xIdxWrap = this.sectionTree.sectionWidth;
        
        int sectionZSplit = Math.min(sectionZStart + this.sectionTree.sectionWidth - tableZStart, sectionZEnd);
        int sectionXSplit = Math.min(sectionXStart + this.sectionTree.sectionWidth - tableXStart, sectionXEnd);
        
        int sectionZ = sectionZStart;
        int sectionZMax = sectionZSplit;
        int zIdxOffset = zIdxStart;
        while (true) {
            if (zIdxOffset >= zIdxWrap && sectionZMax != sectionZEnd) {
                zIdxOffset = 0;
                sectionZ = sectionZMax;
                sectionZMax = sectionZEnd;
            }
        
            if (sectionZ >= sectionZEnd) {
                break;
            }
        
            int sectionX = sectionXStart;
            int sectionXMax = sectionXSplit;
            int xIdxOffset = xIdxStart;
            while (true) {
                if (xIdxOffset >= xIdxWrap && sectionXMax != sectionXEnd) {
                    xIdxOffset = 0;
                    sectionX = sectionXMax;
                    sectionXMax = sectionXEnd;
                }
            
                if (sectionX >= sectionXEnd) {
                    break;
                }
                
                if (!this.isChunkInDrawDistance(sectionX, sectionZ)) {
                    int yIdxIncrement = this.sectionTree.sectionWidthSquared;
                    int yIdxOffset = 0;
                    for (int sectionY = this.sectionTree.sectionHeightMin; sectionY < this.sectionTree.sectionHeightMax; sectionY++, yIdxOffset += yIdxIncrement) {
                        this.sectionVisibilityBitsPass1.unset(yIdxOffset + zIdxOffset + xIdxOffset);
                    }
                }
            
                sectionX++;
                xIdxOffset += xIdxIncrement;
            }
        
            sectionZ++;
            zIdxOffset += zIdxIncrement;
        }
    }
    
    private void occlusionCullAndFillLists(
            boolean useOcclusion,
            final int sectionZStart,
            final int sectionXStart,
            final int sectionZEnd,
            final int sectionXEnd
    ) {
        int visibleChunksCount = 1;
        int visibleChunksQueue = 0;
        
        short traversalOverride = (short) (useOcclusion ? 0 : -1);
        int startSectionIdx = this.sectionTree.getSectionIdx(
                this.sectionTree.camera.getSectionX(),
                this.sectionTree.camera.getSectionY(),
                this.sectionTree.camera.getSectionZ()
        );
        
        boolean fallback = startSectionIdx == SectionTree.NULL_INDEX;
        
        if (fallback) {
            this.getStartingNodesFallback(
                    this.sectionTree.camera.getSectionX(),
                    this.sectionTree.camera.getSectionY(),
                    this.sectionTree.camera.getSectionZ()
            );
        } else {
            this.sortedSections[0] = startSectionIdx;
            this.sectionTraversalData[startSectionIdx] = -1; // All outbound directions
        }
        
        // TODO:FIXME: FALLBACK
        while (visibleChunksQueue != visibleChunksCount) {
            int sectionIdx = this.sortedSections[visibleChunksQueue++];
            
            byte flags = this.sectionTree.sectionFlagData[sectionIdx];
            this.sortedSectionLists.addSectionIdx(sectionIdx, flags);
    
            this.sectionVisibilityBitsPass1.unset(sectionIdx); // Performance hack, means it ignores sections if its already visited them
            this.sectionVisibilityBitsPass2.set(sectionIdx);
            short traversalData = (short) (this.sectionTraversalData[sectionIdx] | traversalOverride);
            this.sectionTraversalData[sectionIdx] = 0; // Reset the traversalData, meaning don't need to fill the array
            traversalData &= ((traversalData >> 8) & 0xFF) | 0xFF00; // Apply inbound chunk filter to prevent backwards traversal
            
            for (int outgoingDir = 0; outgoingDir < DirectionUtil.COUNT; outgoingDir++) {
                if ((traversalData & (1 << outgoingDir)) == 0) {
                    continue;
                }
                
                //TODO: check that the direction is facing away from the camera (dot product less positive or something)
                int neighborSectionIdx = this.getAdjacentIdx(sectionIdx, outgoingDir, sectionZStart, sectionXStart, sectionZEnd, sectionXEnd);
                if (neighborSectionIdx == SectionTree.NULL_INDEX || !this.sectionVisibilityBitsPass1.get(neighborSectionIdx)) {
                    continue;
                }
                
                short neighborTraversalData = this.sectionTraversalData[neighborSectionIdx];
                
                if (neighborTraversalData == 0) {
                    this.sortedSections[visibleChunksCount++] = neighborSectionIdx;
                    neighborTraversalData |= (short) (1 << 15) | (traversalData & 0xFF00);
                }
                
                int inboundDir = DirectionUtil.getOppositeId(outgoingDir);
                neighborTraversalData |= this.getVisibilityData(neighborSectionIdx, inboundDir);
                neighborTraversalData &= ~(1 << (8 + inboundDir)); // Un mark incoming direction
                this.sectionTraversalData[neighborSectionIdx] = neighborTraversalData;
            }
        }
    }
    
    private void getStartingNodesFallback(int sectionX, int sectionY, int sectionZ) {
        int direction = sectionY < this.sectionTree.sectionHeightMin ? Direction.UP.getId() : Direction.DOWN.getId();
        int inDirection = DirectionUtil.getOppositeId(direction);
        // in theory useless
        int mask = 1 << direction;
        
        // clear out lists before running
        for (IntList sectionList : this.fallbackSectionLists) {
            sectionList.clear();
        }

        // M M M B J J J
        // M M I B F J J
        // M I I B F F J
        // E E E A C C C
        // L H H D G G K
        // L L H D G K K
        // L L L D K K K

        // A
        this.tryAddFallbackNode(
                this.sectionTree.getSectionIdx(sectionX, sectionY, sectionZ),
                inDirection,
                (byte) -1,
                this.fallbackSectionLists[0]
        );

        for (int distance = 1; distance <= this.chunkViewDistance; distance++) {
            IntList inner = this.fallbackSectionLists[distance];

            // nodes are checked at the following distances:
            // . . . 3 . . .
            // . . . 2 . . .
            // . . . 1 . . .
            // 3 2 1 . 1 2 3
            // . . . 1 . . .
            // . . . 2 . . .
            // . . . 3 . . .

            {
                // handle the mayor axis
                // B (north z-)
                this.tryAddFallbackNode(
                        this.sectionTree.getSectionIdx(sectionX, sectionY, sectionZ - distance),
                        inDirection,
                        (byte) (mask | XN | ZN | XP),
                        inner
                );
                // C (east x+)
                this.tryAddFallbackNode(
                        this.sectionTree.getSectionIdx(sectionX + distance, sectionY, sectionZ),
                        inDirection,
                        (byte) (mask | XP | ZN | ZP),
                        inner
                );
                // D (south z+)
                this.tryAddFallbackNode(
                        this.sectionTree.getSectionIdx(sectionX, sectionY, sectionZ + distance),
                        inDirection,
                        (byte) (mask | XP | ZP | XN),
                        inner
                );
                // E (west x-)
                this.tryAddFallbackNode(
                        this.sectionTree.getSectionIdx(sectionX - distance, sectionY, sectionZ),
                        inDirection,
                        (byte) (mask | XN | ZP | ZN),
                        inner
                );
            }

            // nodes are checked at the following distances:
            // . . . . . . .
            // . . 3 . 3 . .
            // . 3 2 . 2 3 .
            // . . . . . . .
            // . 3 2 . 2 3 .
            // . . 3 . 3 . .
            // . . . . . . .

            for (int dx = 1; dx < distance; dx++) {
                // handle the inside of the corners areas
                int dz = distance - dx;

                // F (northeast x+ z-)
                this.tryAddFallbackNode(
                        this.sectionTree.getSectionIdx(sectionX + dx, sectionY, sectionZ - dz),
                        inDirection,
                        (byte) (mask | XP | ZN),
                        inner
                );
                // G (southeast x+ z+)
                this.tryAddFallbackNode(
                        this.sectionTree.getSectionIdx(sectionX + dx, sectionY, sectionZ + dz),
                        inDirection,
                        (byte) (mask | XP | ZP),
                        inner
                );
                // H (southwest x- z+)
                this.tryAddFallbackNode(
                        this.sectionTree.getSectionIdx(sectionX - dx, sectionY, sectionZ + dz),
                        inDirection,
                        (byte) (mask | XN | ZP),
                        inner
                );
                // I (northwest x- z-)
                this.tryAddFallbackNode(
                        this.sectionTree.getSectionIdx(sectionX - dx, sectionY, sectionZ - dz),
                        inDirection,
                        (byte) (mask | XN | ZN),
                        inner
                );
            }
        }

        for (int distance = 1; distance <= this.chunkViewDistance; distance++) {
            // nodes are checked at the following distances:
            // 1 2 3 . 3 2 1
            // 2 3 . . . 3 2
            // 3 . . . . . 3
            // . . . . . . .
            // 3 . . . . . 3
            // 2 3 . . . 3 2
            // 1 2 3 . 3 2 1

            IntList outer = this.fallbackSectionLists[2 * this.chunkViewDistance - distance + 1];

            for (int i = 0; i < distance; i++) {
                int dx = this.chunkViewDistance - i;
                int dz = this.chunkViewDistance - distance + i + 1;

                // J (northeast x+ z-)
                this.tryAddFallbackNode(
                        this.sectionTree.getSectionIdx(sectionX + dx, sectionY, sectionZ - dz),
                        inDirection,
                        (byte) (mask | XP | ZN),
                        outer
                );
                // K (southeast x+ z+)
                this.tryAddFallbackNode(
                        this.sectionTree.getSectionIdx(sectionX + dx, sectionY, sectionZ + dz),
                        inDirection,
                        (byte) (mask | XP | ZP),
                        outer
                );
                // L (southwest x- z+)
                this.tryAddFallbackNode(
                        this.sectionTree.getSectionIdx(sectionX - dx, sectionY, sectionZ + dz),
                        inDirection,
                        (byte) (mask | XN | ZP),
                        outer
                );
                // M (northwest x- z-)
                this.tryAddFallbackNode(
                        this.sectionTree.getSectionIdx(sectionX - dx, sectionY, sectionZ - dz),
                        inDirection,
                        (byte) (mask | XN | ZN),
                        outer
                );
            }
        }
    }
    
    private void tryAddFallbackNode(int sectionIdx, int direction, byte directionMask, IntList sectionList) {
        if (sectionIdx != SectionTree.NULL_INDEX && this.sectionVisibilityBitsPass1.get(sectionIdx)) {
            sectionList.add(sectionIdx);
            
            byte visible = this.getVisibilityData(sectionIdx, direction);
            
            // TODO
//            this.allowedTraversalDirections[sectionIdx] = directionMask;
//            this.visibleTraversalDirections[sectionIdx] = (byte) (directionMask & visible);
        }
    }
    
    public boolean isSectionVisible(int x, int y, int z) {
        int sectionIdx = this.sectionTree.getSectionIdx(x, y, z);
        
        if (sectionIdx == SectionTree.NULL_INDEX) {
            return false;
        }
        
        return this.sectionVisibilityBitsPass2.get(sectionIdx);
    }
    
    public boolean isChunkInDrawDistance(int x, int z) {
        double centerX = ChunkSectionPos.getBlockCoord(x) + 8.0;
        double centerZ = ChunkSectionPos.getBlockCoord(z) + 8.0;
        Vec3d cameraPos = this.sectionTree.camera.getPos();
        double xDist = cameraPos.getX() - centerX;
        double zDist = cameraPos.getZ() - centerZ;
        
        return (xDist * xDist) + (zDist * zDist) <= this.squaredFogDistance;
    }
    
    public void setVisibilityData(int sectionIdx, ChunkOcclusionData data) {
        long bits = 0;

        // The underlying data is already formatted to what we need, so we can just grab the long representation and work with that
        if (data != null) {
            BitSet bitSet = data.visibility;
            if (!bitSet.isEmpty()) {
                bits = bitSet.toLongArray()[0];
            }
        }

        for (int fromIdx = 0; fromIdx < DirectionUtil.COUNT; fromIdx++) {
            byte toBits = (byte) (bits & VIS_DATA_MASK);
            bits >>= DirectionUtil.COUNT;

            this.sectionDirVisibilityData[(sectionIdx * DirectionUtil.COUNT) + fromIdx] = toBits;
        }
    }
    
    private byte getVisibilityData(int sectionIdx, int incomingDirection) {
        return this.sectionDirVisibilityData[(sectionIdx * DirectionUtil.COUNT) + incomingDirection];
    }
    
    public int getAdjacentIdx(
            int sectionIdx,
            int directionId,
            int tableZStart,
            int tableXStart,
            int tableZEnd,
            int tableXEnd
    ) {
        int tableY = sectionIdx >> this.sectionTree.idxYShift;
        int tableZ = (sectionIdx >> this.sectionTree.idxZShift) & this.sectionTree.sectionWidthMask;
        int tableX = sectionIdx & this.sectionTree.sectionWidthMask;
        
        // do some terrible bit hacks to decrement and increment the index in the correct direction
        return switch (directionId) {
            case DirectionUtil.X_MIN -> tableX == tableXStart
                                        ? SectionTree.NULL_INDEX
                                        : (sectionIdx & ~this.sectionTree.idxXMask) | ((sectionIdx - 1) & this.sectionTree.idxXMask);
            case DirectionUtil.X_PLUS -> tableX == tableXEnd - 1
                                         ? SectionTree.NULL_INDEX
                                         : (sectionIdx & ~this.sectionTree.idxXMask) | ((sectionIdx + 1) & this.sectionTree.idxXMask);
            case DirectionUtil.Z_MIN -> tableZ == tableZStart
                                        ? SectionTree.NULL_INDEX
                                        : (sectionIdx & ~this.sectionTree.idxZMask) | ((sectionIdx - this.sectionTree.sectionWidth) & this.sectionTree.idxZMask);
            case DirectionUtil.Z_PLUS -> tableZ == tableZEnd - 1
                                         ? SectionTree.NULL_INDEX
                                         : (sectionIdx & ~this.sectionTree.idxZMask) | ((sectionIdx + this.sectionTree.sectionWidth) & this.sectionTree.idxZMask);
    
            case DirectionUtil.Y_MIN -> tableY == 0
                                        ? SectionTree.NULL_INDEX
                                        : (sectionIdx & ~this.sectionTree.idxYMask) | ((sectionIdx - this.sectionTree.sectionWidthSquared) & this.sectionTree.idxYMask);
            case DirectionUtil.Y_PLUS -> tableY == this.sectionTree.sectionHeight - 1
                                         ? SectionTree.NULL_INDEX
                                         : (sectionIdx & ~this.sectionTree.idxYMask) | ((sectionIdx + this.sectionTree.sectionWidthSquared) & this.sectionTree.idxYMask);
            default -> throw new IllegalStateException("Unknown direction ID: " + directionId);
        };
    }
}