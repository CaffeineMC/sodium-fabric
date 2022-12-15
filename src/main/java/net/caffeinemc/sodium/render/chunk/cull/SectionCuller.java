package net.caffeinemc.sodium.render.chunk.cull;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Arrays;
import java.util.Iterator;
import net.caffeinemc.gfx.util.misc.MathUtil;
import net.caffeinemc.sodium.interop.vanilla.math.frustum.Frustum;
import net.caffeinemc.sodium.render.chunk.RenderSection;
import net.caffeinemc.sodium.util.DirectionUtil;
import net.caffeinemc.sodium.util.collections.BitArray;
import net.minecraft.client.render.chunk.ChunkOcclusionData;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class SectionCuller {
    
    private final SectionTree sectionTree;
    private final int chunkViewDistance;
    private final double squaredFogDistance;
    
    private final BitArray sectionVisibilityBits;
    private final BitArray sectionOcclusionVisibilityBits;
    
    private final byte[] sectionDirVisibilityData;
    
    /*
     * The outgoing directions out of the chunk
     */
    private final byte[] visibleTraversalDirections;
    /*
     * The directions that the bfs is still allowed to check
     */
    private final byte[] allowedTraversalDirections;
    
    private final IntList[] fallbackSectionLists;
    
    // Chunks are grouped by manhattan distance to the start chunk, and given
    // the fact that the chunk graph is bipartite, it's possible to simply
    // alternate the lists to form a queue
    private final int[] queue1;
    private final int[] queue2;
    
    private final int[] sortedVisibleSections;
    private int visibleSectionCount;
    
    public SectionCuller(SectionTree sectionTree, int chunkViewDistance) {
        this.sectionTree = sectionTree;
        this.chunkViewDistance = chunkViewDistance;
        this.squaredFogDistance = MathHelper.square((chunkViewDistance + 1) * 16.0);
        
        int maxSize = (MathHelper.square(chunkViewDistance * 2 + 1) * sectionTree.sectionHeight);
        this.queue1 = new int[maxSize];
        this.queue2 = new int[maxSize];
        this.sortedVisibleSections = new int[maxSize];
        
        this.fallbackSectionLists = new IntList[chunkViewDistance * 2 + 1];
        // the first list will have a known size of 1 always
        this.fallbackSectionLists[0] = new IntArrayList(1);
        for (int i = 1; i < this.fallbackSectionLists.length; i++) {
            this.fallbackSectionLists[i] = new IntArrayList();
        }
        
        this.visibleTraversalDirections = new byte[sectionTree.getSectionTableSize()];
        this.allowedTraversalDirections = new byte[sectionTree.getSectionTableSize()];
        this.sectionDirVisibilityData = new byte[sectionTree.getSectionTableSize() * DirectionUtil.COUNT];
        this.sectionVisibilityBits = new BitArray(sectionTree.getSectionTableSize());
        this.sectionOcclusionVisibilityBits = new BitArray(sectionTree.getSectionTableSize());
    }
    
    public void calculateVisibleSections(
            Frustum frustum,
            boolean useOcclusionCulling
    ) {
        this.sectionVisibilityBits.fill(false);
        this.sectionOcclusionVisibilityBits.fill(false);
    
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
            this.occlusionCull(
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
                        this.sectionVisibilityBits.copy(
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
            this.sectionVisibilityBits.set(sectionIdx);
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
                        this.sectionVisibilityBits.unset(yIdxOffset + zIdxOffset + xIdxOffset);
                    }
                }
            
                sectionX++;
                xIdxOffset += xIdxIncrement;
            }
        
            sectionZ++;
            zIdxOffset += zIdxIncrement;
        }
    }

    private void occlusionCull(
            boolean useOcclusionCulling,
            final int sectionZStart,
            final int sectionXStart,
            final int sectionZEnd,
            final int sectionXEnd
    ) {
        // It's possible to improve culling for spectator mode players in walls,
        // but the time cost to update this algorithm, bugfix it and maintain it
        // on top of the potential slowdown of the normal path seems to outweigh
        // any gains in those rare circumstances
        int visibilityOverride = useOcclusionCulling ? 0 : -1;
    
        int tableZStart = sectionZStart & this.sectionTree.sectionWidthMask;
        int tableXStart = sectionXStart & this.sectionTree.sectionWidthMask;
        int tableZEnd = sectionZEnd & this.sectionTree.sectionWidthMask;
        int tableXEnd = sectionXEnd & this.sectionTree.sectionWidthMask;
    
        this.visibleSectionCount = 0;
        int currentQueueSize = 0;
        int nextQueueSize = 0;
        
        int[] currentQueue = this.queue1;
        int[] nextQueue = this.queue2;
        
        int originSectionX = this.sectionTree.camera.getSectionX();
        int originSectionY = this.sectionTree.camera.getSectionY();
        int originSectionZ = this.sectionTree.camera.getSectionZ();
    
        int startSectionIdx = this.sectionTree.getSectionIdx(originSectionX, originSectionY, originSectionZ);
        
        boolean fallback = startSectionIdx == SectionTree.OUT_OF_BOUNDS_INDEX;
        if (fallback) {
            this.getStartingNodesFallback(originSectionX, originSectionY, originSectionZ);
        } else {
            currentQueue[0] = startSectionIdx;
            currentQueueSize = 1;

            byte visible = 0;

            for (int direction = 0; direction < DirectionUtil.COUNT; direction++) {
                visible |= this.getVisibilityData(startSectionIdx, direction);
            }

            this.allowedTraversalDirections[startSectionIdx] = (byte) -1;
            this.visibleTraversalDirections[startSectionIdx] = visible;
        }

        int fallbackIndex = 0;

        while (true) {
            if (fallback && fallbackIndex < this.fallbackSectionLists.length) {
                IntList nodes = this.fallbackSectionLists[fallbackIndex];
                int count = nodes.size();
                nodes.getElements(0, currentQueue, currentQueueSize, count);
                currentQueueSize += count;
                fallbackIndex++;

                // Make sure that there are entries in the queue.
            } else if (currentQueueSize == 0) {
                break;
            }

            for (int i = 0; i < currentQueueSize; i++) {
                int sectionIdx = currentQueue[i];

                this.sortedVisibleSections[this.visibleSectionCount++] = sectionIdx;
                this.sectionOcclusionVisibilityBits.set(sectionIdx);

                byte allowedTraversalDirection = this.allowedTraversalDirections[sectionIdx];
                byte visibleTraversalDirection = this.visibleTraversalDirections[sectionIdx];

                for (int outgoingDirection = 0; outgoingDirection < DirectionUtil.COUNT; outgoingDirection++) {
                    if ((visibleTraversalDirection & (1 << outgoingDirection)) == 0) {
                        continue;
                    }

                    int adjacentNodeIdx = this.getAdjacentIdx(
                            sectionIdx,
                            outgoingDirection,
                            tableZStart,
                            tableXStart,
                            tableZEnd,
                            tableXEnd
                    );

                    // TEMP
                    if (adjacentNodeIdx < -1 || sectionIdx < 0) {
                        int adjacentNodeIdx2 = this.getAdjacentIdx(
                                sectionIdx,
                                outgoingDirection,
                                tableZStart,
                                tableXStart,
                                tableZEnd,
                                tableXEnd
                        );
                        System.out.println(adjacentNodeIdx2);
                    }
                    // END TEMP

                    if (adjacentNodeIdx == SectionTree.OUT_OF_BOUNDS_INDEX || !this.sectionVisibilityBits.get(adjacentNodeIdx)) {
                        continue;
                    }

                    int reverseDirection = DirectionUtil.getOppositeId(outgoingDirection);

                    int visible = this.getVisibilityData(adjacentNodeIdx, reverseDirection) | visibilityOverride;
                    // prevent further iterations to backtrack
                    int newAllowed = allowedTraversalDirection & ~(1 << reverseDirection);

                    if (this.allowedTraversalDirections[adjacentNodeIdx] == 0) {
                        nextQueue[nextQueueSize++] = adjacentNodeIdx;
                        if (newAllowed == 0) {
                            // TODO: I don't think it's mathematically possible to trigger this
                            // avoid adding it twice to the list!
                            newAllowed = 0b1000_0000; // not 0 but doesn't set any of the direction bits
                        }
                    }

                    // TODO: I think newAllowed can just be set here, the old
                    //  value is always 0 if the node hasn't been seen yet, or
                    //  the same value if it has, as newAllowed is just a bitmask
                    //  telling exactly which of the 6 connected chunks are further
                    //  away in manhattan distance
                    this.allowedTraversalDirections[adjacentNodeIdx] |= newAllowed;
                    this.visibleTraversalDirections[adjacentNodeIdx] |= newAllowed & visible;
                }
            }

            // swap and reset
            int[] temp = currentQueue;
            currentQueue = nextQueue;
            nextQueue = temp;
            
            currentQueueSize = nextQueueSize;
            nextQueueSize = 0;
        }

        Arrays.fill(this.allowedTraversalDirections, (byte) 0);
        Arrays.fill(this.visibleTraversalDirections, (byte) 0);
    }

    private static final int XP = 1 << DirectionUtil.X_PLUS;
    private static final int XN = 1 << DirectionUtil.X_MIN;
    private static final int ZP = 1 << DirectionUtil.Z_PLUS;
    private static final int ZN = 1 << DirectionUtil.Z_MIN;
    
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
        if (sectionIdx != SectionTree.OUT_OF_BOUNDS_INDEX && this.sectionVisibilityBits.get(sectionIdx)) {
            sectionList.add(sectionIdx);
            
            int visible = this.getVisibilityData(sectionIdx, direction);
            
            this.allowedTraversalDirections[sectionIdx] = directionMask;
            this.visibleTraversalDirections[sectionIdx] = (byte) (directionMask & visible);
        }
    }
    
    public Iterator<RenderSection> getVisibleSectionIterator() {
        return new SortedVisibleSectionIterator();
//        return new VisibleSectionIterator();
    }
    
    private class VisibleSectionIterator implements Iterator<RenderSection> {
        private int nextIdx;
        
        private VisibleSectionIterator() {
            this.nextIdx = SectionCuller.this.sectionVisibilityBits.nextSetBit(0);
        }
        
        @Override
        public boolean hasNext() {
            return this.nextIdx != -1;
        }
        
        @Override
        public RenderSection next() {
            RenderSection section = SectionCuller.this.sectionTree.sections[this.nextIdx];
            this.nextIdx = SectionCuller.this.sectionVisibilityBits.nextSetBit(this.nextIdx + 1);
            return section;
        }
    }
    
    private class SortedVisibleSectionIterator implements Iterator<RenderSection> {
        private int idx;
        
        private SortedVisibleSectionIterator() {
        }
        
        @Override
        public boolean hasNext() {
            return this.idx < SectionCuller.this.visibleSectionCount;
        }
        
        @Override
        public RenderSection next() {
            int trueIdx = SectionCuller.this.sortedVisibleSections[this.idx++];
            return SectionCuller.this.sectionTree.sections[trueIdx];
        }
    }
    
    public boolean isSectionVisible(int x, int y, int z) {
        int sectionIdx = this.sectionTree.getSectionIdx(x, y, z);
        
        if (sectionIdx == SectionTree.OUT_OF_BOUNDS_INDEX) {
            return false;
        }
        
        return this.sectionOcclusionVisibilityBits.get(sectionIdx);
    }
    
    public boolean isChunkInDrawDistance(int x, int z) {
        double centerX = ChunkSectionPos.getBlockCoord(x) + 8.0;
        double centerZ = ChunkSectionPos.getBlockCoord(z) + 8.0;
        Vec3d cameraPos = this.sectionTree.camera.getPos();
        double xDist = cameraPos.getX() - centerX;
        double zDist = cameraPos.getZ() - centerZ;
        
        return (xDist * xDist) + (zDist * zDist) <= this.squaredFogDistance;
    }
    
    public void setVisibilityData(int x, int y, int z, ChunkOcclusionData data) {
        int sectionIdx = this.sectionTree.getSectionIdx(x, y, z);
        
        if (sectionIdx == SectionTree.OUT_OF_BOUNDS_INDEX) {
            return;
        }
        
        for (var from : DirectionUtil.ALL_DIRECTIONS) {
            int bits = 0;
            
            if (data != null) {
                for (var to : DirectionUtil.ALL_DIRECTIONS) {
                    if (data.isVisibleThrough(from, to)) {
                        bits |= 1 << to.ordinal();
                    }
                }
            }
            
            this.sectionDirVisibilityData[(sectionIdx * DirectionUtil.COUNT) + from.ordinal()] = (byte) bits;
        }
    }
    
    private int getVisibilityData(int sectionIdx, int incomingDirection) {
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
        
        return switch (directionId) {
            case DirectionUtil.X_MIN -> tableX == tableXStart
                                        ? SectionTree.OUT_OF_BOUNDS_INDEX
                                        : (sectionIdx & ~this.sectionTree.idxXMask) | ((sectionIdx - 1) & this.sectionTree.idxXMask);
            case DirectionUtil.X_PLUS -> tableX == tableXEnd - 1
                                         ? SectionTree.OUT_OF_BOUNDS_INDEX
                                         : (sectionIdx & ~this.sectionTree.idxXMask) | ((sectionIdx + 1) & this.sectionTree.idxXMask);
    
            case DirectionUtil.Z_MIN -> tableZ == tableZStart
                                        ? SectionTree.OUT_OF_BOUNDS_INDEX
                                        : (sectionIdx & ~this.sectionTree.idxZMask) | ((sectionIdx - this.sectionTree.sectionWidth) & this.sectionTree.idxZMask);
            case DirectionUtil.Z_PLUS -> tableZ == tableZEnd - 1
                                         ? SectionTree.OUT_OF_BOUNDS_INDEX
                                         : (sectionIdx & ~this.sectionTree.idxZMask) | ((sectionIdx + this.sectionTree.sectionWidth) & this.sectionTree.idxZMask);
    
            case DirectionUtil.Y_MIN -> tableY == 0
                                        ? SectionTree.OUT_OF_BOUNDS_INDEX
                                        : (sectionIdx & ~this.sectionTree.idxYMask) | ((sectionIdx - this.sectionTree.sectionWidthSquared) & this.sectionTree.idxYMask);
            case DirectionUtil.Y_PLUS -> tableY == this.sectionTree.sectionHeight - 1
                                         ? SectionTree.OUT_OF_BOUNDS_INDEX
                                         : (sectionIdx & ~this.sectionTree.idxYMask) | ((sectionIdx + this.sectionTree.sectionWidthSquared) & this.sectionTree.idxYMask);
            default -> throw new IllegalStateException("Unknown direction ID: " + directionId);
        };
    }
}