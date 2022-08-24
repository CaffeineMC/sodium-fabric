package net.caffeinemc.sodium.render.chunk.occlusion;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Iterator;
import net.caffeinemc.sodium.interop.vanilla.math.frustum.Frustum;
import net.caffeinemc.sodium.render.chunk.RenderSection;
import net.caffeinemc.sodium.util.DirectionUtil;
import net.caffeinemc.sodium.util.collections.BitArray;
import net.minecraft.client.render.chunk.ChunkOcclusionData;
import net.minecraft.util.math.ChunkSectionPos;

public class SectionCuller {
    
    private final SectionTree sectionTree;
    
    // 2 bit arrays: 1 for frustum, one for occlusion
    private final BitArray sectionVisibilityBits;
    private final byte[] sectionDirVisibilityData;
    
    /*
     * The outgoing directions out of the chunk
     */
    private final byte[] visibleTraversalDirections;
    /*
     * The directions that the bfs is still allowed to check
     */
    private final byte[] allowedTraversalDirections;
    
    // Chunks are grouped by manhattan distance to the start chunk, and given
    // the fact that the chunk graph is bipartite, it's possible to simply
    // alternate the lists to form a queue
    private IntList currentQueue;
    private IntList nextQueue;
    
    public SectionCuller(SectionTree sectionTree) {
        this.sectionTree = sectionTree;
        
        // TODO: correctly predict size, maybe inline array and keep position?
        this.currentQueue = new IntArrayList(128);
        this.nextQueue = new IntArrayList(128);
        
        this.visibleTraversalDirections = new byte[sectionTree.getSectionTableSize()];
        this.allowedTraversalDirections = new byte[sectionTree.getSectionTableSize()];
        this.sectionDirVisibilityData = new byte[sectionTree.getSectionTableSize() * DirectionUtil.COUNT];
        this.sectionVisibilityBits = new BitArray(sectionTree.getSectionTableSize());
    }
    
    public void calculateVisibleSections(
            Frustum frustum,
            boolean useOcclusionCulling
    ) {
        this.sectionVisibilityBits.fill(false);
    
        if (this.sectionTree.getLoadedSections() != 0) {
            this.sectionVisibilityBits.copy(this.sectionTree.sectionExistenceBits, 0, this.sectionVisibilityBits.capacity());
//            this.frustumCull(frustum);
//            this.fogCull();
            // still need to do this to maintain ordering between sections
//            this.occlusionCull(useOcclusionCulling);
        }
    }
    
    private void frustumCull(Frustum frustum) {
        // also the xIdxIncrement
        int nodeSectionLength = 1 << this.sectionTree.maxDepth;
    
        int yIdxIncrement = nodeSectionLength * this.sectionTree.sectionWidthSquared;
        int zIdxIncrement = nodeSectionLength * this.sectionTree.sectionWidth;
    
        // TODO: shrink this size with fog culling
        int tableYStart = Math.floorMod(this.sectionTree.camera.getSectionY() - this.sectionTree.sectionHeightOffset, this.sectionTree.sectionHeight);
        int tableZStart = Math.floorMod(this.sectionTree.camera.getSectionZ() - this.sectionTree.sectionWidthOffset, this.sectionTree.sectionWidth);
        int tableXStart = Math.floorMod(this.sectionTree.camera.getSectionX() - this.sectionTree.sectionWidthOffset, this.sectionTree.sectionWidth);
        int tableYEnd = tableYStart + this.sectionTree.sectionHeight;
        int tableZEnd = tableZStart + this.sectionTree.sectionWidth;
        int tableXEnd = tableXStart + this.sectionTree.sectionWidth;
        
        int tableYMin = tableYStart;
        int tableZMin = tableZStart;
        int tableXMin = tableXStart;
        int tableYMax = this.sectionTree.sectionHeight;
        int tableZMax = this.sectionTree.sectionWidth;
        int tableXMax = this.sectionTree.sectionWidth;
    
        int tableY = tableYStart;
        int yIdxOffset = tableY * this.sectionTree.sectionWidthSquared;
        while (true) {
            if (tableY >= this.sectionTree.sectionHeight) {
               yIdxOffset -= this.sectionTree.sectionTableSize;
               tableYMin = 0;
               tableYMax = tableYEnd;
            }
    
            if (tableY >= tableYEnd) {
                break;
            }
    
            int tableZ = tableZStart;
            int zIdxOffset = tableZ * this.sectionTree.sectionWidth;
            while (true) {
                if (tableZ >= this.sectionTree.sectionWidth) {
                   zIdxOffset -= this.sectionTree.sectionWidthSquared;
                   tableZMin = 0;
                   tableZMax = tableZEnd;
                }
    
                if (tableZ >= tableZEnd) {
                    break;
                }
    
                int tableX = tableXStart;
                int xIdxOffset = tableX;
                while (true) {
                    if (tableX >= this.sectionTree.sectionWidth) {
                       xIdxOffset -= this.sectionTree.sectionWidth;
                       tableXMin = 0;
                       tableXMax = tableXEnd;
                    }
    
                    if (tableX >= tableXEnd) {
                        break;
                    }
                    
                    this.checkNode(
                           this.sectionVisibilityBits,
                           frustum,
                           tableY,
                           tableZ,
                           tableX,
                           tableYMin,
                           tableZMin,
                           tableXMin,
                           tableYMax,
                           tableZMax,
                           tableXMax,
                           this.sectionTree.maxDepth,
                           yIdxOffset + zIdxOffset + xIdxOffset
                    );
                    
                    tableX += nodeSectionLength;
                    xIdxOffset += nodeSectionLength;
                }
                
                tableZ += nodeSectionLength;
                zIdxOffset += zIdxIncrement;
            }
            
            tableY += nodeSectionLength;
            yIdxOffset += yIdxIncrement;
        }
    }
    
    @SuppressWarnings("SuspiciousNameCombination")
    private void checkNode(
            BitArray sectionVisibilityBits,
            Frustum frustum,
            int tableY,
            int tableZ,
            int tableX,
            int tableYMin,
            int tableZMin,
            int tableXMin,
            int tableYMax,
            int tableZMax,
            int tableXMax,
            int depth,
            int sectionIdx
    ) {
//        if (this.sectionTree.getSectionIdxUnchecked(tableX, tableY, tableZ) != sectionIdx) {
//            System.out.println("bad");
//        }
        
        if (depth == 0 && !this.sectionTree.sectionExistenceBits.get(sectionIdx)) {
            // skip if the section doesn't exist
            return;
        }
        
        int nodeSectionLength = 1 << depth;
        // end exclusive
        int tableYEnd = Math.min(tableY + nodeSectionLength, tableYMax);
        int tableZEnd = Math.min(tableZ + nodeSectionLength, tableZMax);
        int tableXEnd = Math.min(tableX + nodeSectionLength, tableXMax);
        
        tableY = Math.max(tableY, tableYMin);
        tableZ = Math.max(tableZ, tableZMin);
        tableX = Math.max(tableX, tableXMin);
    
        float minY = (float) ChunkSectionPos.getBlockCoord(tableY + this.sectionTree.camera.getSectionY() - this.sectionTree.sectionHeightOffset);
        float minZ = (float) ChunkSectionPos.getBlockCoord(tableZ + this.sectionTree.camera.getSectionZ() - this.sectionTree.sectionWidthOffset);
        float minX = (float) ChunkSectionPos.getBlockCoord(tableX + this.sectionTree.camera.getSectionX() - this.sectionTree.sectionWidthOffset);
        float maxY = (float) ChunkSectionPos.getBlockCoord(tableYEnd + this.sectionTree.camera.getSectionY() - this.sectionTree.sectionHeightOffset);
        float maxZ = (float) ChunkSectionPos.getBlockCoord(tableZEnd + this.sectionTree.camera.getSectionZ() - this.sectionTree.sectionWidthOffset);
        float maxX = (float) ChunkSectionPos.getBlockCoord(tableXEnd + this.sectionTree.camera.getSectionX() - this.sectionTree.sectionWidthOffset);

        int frustumTestResult = frustum.testBox(minX, minY, minZ, maxX, maxY, maxZ);
        
        if (depth == 0) {
            if (frustumTestResult == Frustum.INTERSECT || frustumTestResult == Frustum.INSIDE) {
                // we already tested that it does exist, so we can unconditionally set
                sectionVisibilityBits.set(sectionIdx);
            }
        } else {
            switch (frustumTestResult) {
                case Frustum.INTERSECT -> {
                    int childDepth = depth - 1;
                    int childSectionLength = nodeSectionLength >> 1;
                    
                    int yIdxIncrement = childSectionLength * this.sectionTree.sectionWidthSquared;
                    int zIdxIncrement = childSectionLength * this.sectionTree.sectionWidth;
                    
                    for (int yIdxOffset = 0; tableY < tableYEnd; tableY += childSectionLength, yIdxOffset += yIdxIncrement) {
                        for (int zIdxOffset = 0; tableZ < tableZEnd; tableZ += childSectionLength, zIdxOffset += zIdxIncrement) {
                            for (int xIdxOffset = 0; tableX < tableXEnd; tableX += childSectionLength, xIdxOffset += childSectionLength) {
                                this.checkNode(
                                        sectionVisibilityBits,
                                        frustum,
                                        tableY,
                                        tableZ,
                                        tableX,
                                        tableYMin,
                                        tableZMin,
                                        tableXMin,
                                        tableYMax,
                                        tableZMax,
                                        tableXMax,
                                        childDepth,
                                        sectionIdx + yIdxOffset + zIdxOffset + xIdxOffset
                                );
                            }
                        }
                    }
                }
                case Frustum.INSIDE -> {
                    for (int yIdxOffset = 0; tableY < tableYEnd; tableY++, yIdxOffset += this.sectionTree.sectionWidthSquared) {
                        for (int zIdxOffset = 0; tableZ < tableZEnd; tableZ++, zIdxOffset += this.sectionTree.sectionWidth) {
                            sectionVisibilityBits.copy(
                                    this.sectionTree.sectionExistenceBits,
                                    sectionIdx + yIdxOffset + zIdxOffset,
                                    sectionIdx + yIdxOffset + zIdxOffset + tableXEnd - tableX
                            );
                        }
                    }
                }
            }
        }
    }
    
//    private void fogCull() {
//        switch (RenderSystem.getShaderFogShape()) {
//            case SPHERE -> {}
//            case CYLINDER -> {}
//        }
//    }

//    private void occlusionCull() {
//        // Run the frustum check early so the fallback can use it too
//        this.sectionTree.findVisibleSections(this.sectionVisibilityBits, frustum);
//
//        IntList[] startingNodes = null;
//
//        int startSectionIdx = this.sectionTree.getSectionIdx(
//                ChunkSectionPos.getSectionCoord(origin.getX()),
//                ChunkSectionPos.getSectionCoord(origin.getY()),
//                ChunkSectionPos.getSectionCoord(origin.getZ())
//        );
//
//        if (startSectionIdx == SectionTree.ABSENT_VALUE) {
//            startingNodes = this.getStartingNodesFallback(
//                    this.allowedTraversalDirections,
//                    this.visibleTraversalDirections
//            );
//        } else {
//            this.currentQueue.add(startSectionIdx);
//
//            byte visible = 0;
//
//            for (int direction = 0; direction < DirectionUtil.COUNT; direction++) {
//                visible |= this.getVisibilityData(startSectionIdx, direction);
//            }
//
//            this.allowedTraversalDirections[startSectionIdx] = (byte) -1;
//            this.visibleTraversalDirections[startSectionIdx] = visible;
//        }
//
//
//        // It's possible that due to the near clip plane, the chunk the camera is in,
//        // isn't actually visible, however since we still need to use it as a starting
//        // point for iterating, we make it always visible, which means we need one extra
//        // slot in the array
//        int[] visibleSections = new int[vis.count() + 1];
//        int visibleSectionCount = 0;
//
//        int fallbackIndex = 0;
//
//        while (true) {
//            if (startingNodes != null && fallbackIndex < startingNodes.length) {
//                this.currentQueue.addAll(startingNodes[fallbackIndex]);
//                fallbackIndex++;
//
//                // It's possible that due to unloaded chunks, there entries in
//                // the startingNodes array that are empty
//            } else if (this.currentQueue.isEmpty()) {
//                break;
//            }
//
//            for (int i = 0, length = this.currentQueue.size(); i < length; i++) {
//                int sectionIdx = this.currentQueue.getInt(i);
//
//                visibleSections[visibleSectionCount++] = sectionIdx;
//
//                byte allowedTraversalDirection = this.allowedTraversalDirections[sectionIdx];
//                byte visibleTraversalDirection = this.visibleTraversalDirections[sectionIdx];
//
//                for (int outgoingDirection = 0; outgoingDirection < DirectionUtil.COUNT; outgoingDirection++) {
//                    if ((visibleTraversalDirection & (1 << outgoingDirection)) == 0) {
//                        continue;
//                    }
//
//                    int adjacentNodeId = this.getAdjacent(sectionIdx, outgoingDirection);
//
//                    if (adjacentNodeId == SectionTree.ABSENT_VALUE || !vis.get(adjacentNodeId)) {
//                        continue;
//                    }
//
//                    int reverseDirection = DirectionUtil.getOppositeId(outgoingDirection);
//
//                    int visible = this.getVisibilityData(adjacentNodeId, reverseDirection) | visibilityOverride;
//                    // prevent further iterations to backtrack
//                    int newAllowed = allowedTraversalDirection & ~(1 << reverseDirection);
//
//                    if (this.allowedTraversalDirections[adjacentNodeId] == 0) {
//                        this.nextQueue.add(adjacentNodeId);
//                        if (newAllowed == 0) {
//                            // TODO: I don't think it's mathematically possible to trigger this
//                            // avoid adding it twice to the list!
//                            newAllowed = 0b1000_0000; // not 0 but doesn't set any of the direction bits
//                        }
//                    }
//
//                    // TODO: I think newAllowed can just be set here, the old
//                    //  value is always 0 if the node hasn't been seen yet, or
//                    //  the same value if it has, as newAllowed is just a bitmask
//                    //  telling exactly which of the 6 connected chunks are further
//                    //  away in manhattan distance
//                    this.allowedTraversalDirections[adjacentNodeId] |= newAllowed;
//                    this.visibleTraversalDirections[adjacentNodeId] |= newAllowed & visible;
//                }
//            }
//
//            // swap and reset
//            IntList temp = this.currentQueue;
//            this.currentQueue = this.nextQueue;
//            this.nextQueue = temp;
//            this.nextQueue.clear();
//        }
//
//        Arrays.fill(this.allowedTraversalDirections, (byte) 0);
//        Arrays.fill(this.visibleTraversalDirections, (byte) 0);
//
//        return IntArrayList.wrap(visibleSections, visibleSectionCount);
//    }

//    private static void tryAdd(int sectionIdx, SectionTree tree, int direction, byte directionMask, byte[] allowedTraversalDirections, byte[] visibleTraversalDirections, BitArray visibleSections, IntList sections) {
//        if (sectionIdx != SectionTree.ABSENT_VALUE && visibleSections.get(sectionIdx)) {
//            sections.add(sectionIdx);
//
//            int visible = tree.getVisibilityData(sectionIdx, direction);
//
//            allowedTraversalDirections[sectionIdx] = directionMask;
//            visibleTraversalDirections[sectionIdx] = (byte) (directionMask & visible);
//        }
//    }
//
//    private static final int XP = 1 << DirectionUtil.X_PLUS;
//
//    private static final int XN = 1 << DirectionUtil.X_MIN;
//    private static final int ZP = 1 << DirectionUtil.Z_PLUS;
//    private static final int ZN = 1 << DirectionUtil.Z_MIN;
//    private IntList[] getStartingNodesFallback(BitArray sectionVisibilityBits, byte[] allowedTraversalDirections, byte[] visibleTraversalDirections) {
//        IntList[] sections = new IntList[this.chunkViewDistance * 2 + 1];
//
//        int sectionX = this.sectionTree.getOriginSectionX();
//        int sectionY = this.sectionTree.getOriginSectionY();
//        int sectionZ = this.sectionTree.getOriginSectionZ();
//
//        int direction = sectionY < this.heightLimitView.getBottomSectionCoord() ? Direction.UP.getId() : Direction.DOWN.getId();
//        int inDirection = DirectionUtil.getOppositeId(direction);
//        // in theory useless
//        int mask = 1 << direction;
//
//        // M M M B J J J
//        // M M I B F J J
//        // M I I B F F J
//        // E E E A C C C
//        // L H H D G G K
//        // L L H D G K K
//        // L L L D K K K
//
//        // A
//        tryAdd(
//                this.sectionTree.getSectionIdx(sectionX, sectionY, sectionZ),
//                this.sectionTree,
//                inDirection,
//                (byte) -1,
//                allowedTraversalDirections,
//                visibleTraversalDirections,
//                sectionVisibilityBits,
//                sections[0] = new IntArrayList(1)
//        );
//
//        for (int distance = 1; distance <= this.chunkViewDistance; distance++) {
//            IntList inner = sections[distance] = new IntArrayList();
//
//            // nodes are checked at the following distances:
//            // . . . 3 . . .
//            // . . . 2 . . .
//            // . . . 1 . . .
//            // 3 2 1 . 1 2 3
//            // . . . 1 . . .
//            // . . . 2 . . .
//            // . . . 3 . . .
//
//            {
//                // handle the mayor axis
//                // B (north z-)
//                tryAdd(
//                        this.sectionTree.getSectionIdx(sectionX, sectionY, sectionZ - distance),
//                        this.sectionTree,
//                        inDirection,
//                        (byte) (mask | XN | ZN | XP),
//                        allowedTraversalDirections,
//                        visibleTraversalDirections,
//                        sectionVisibilityBits,
//                        inner
//                );
//                // C (east x+)
//                tryAdd(
//                        this.sectionTree.getSectionIdx(sectionX + distance, sectionY, sectionZ),
//                        this.sectionTree,
//                        inDirection,
//                        (byte) (mask | XP | ZN | ZP),
//                        allowedTraversalDirections,
//                        visibleTraversalDirections,
//                        sectionVisibilityBits,
//                        inner
//                );
//                // D (south z+)
//                tryAdd(
//                        this.sectionTree.getSectionIdx(sectionX, sectionY, sectionZ + distance),
//                        this.sectionTree,
//                        inDirection,
//                        (byte) (mask | XP | ZP | XN),
//                        allowedTraversalDirections,
//                        visibleTraversalDirections,
//                        sectionVisibilityBits,
//                        inner
//                );
//                // E (west x-)
//                tryAdd(
//                        this.sectionTree.getSectionIdx(sectionX - distance, sectionY, sectionZ),
//                        this.sectionTree,
//                        inDirection,
//                        (byte) (mask | XN | ZP | ZN),
//                        allowedTraversalDirections,
//                        visibleTraversalDirections,
//                        sectionVisibilityBits,
//                        inner
//                );
//            }
//
//            // nodes are checked at the following distances:
//            // . . . . . . .
//            // . . 3 . 3 . .
//            // . 3 2 . 2 3 .
//            // . . . . . . .
//            // . 3 2 . 2 3 .
//            // . . 3 . 3 . .
//            // . . . . . . .
//
//            for (int dx = 1; dx < distance; dx++) {
//                // handle the inside of the corners areas
//                int dz = distance - dx;
//
//                // F (northeast x+ z-)
//                tryAdd(
//                        this.sectionTree.getSectionIdx(sectionX + dx, sectionY, sectionZ - dz),
//                        this.sectionTree,
//                        inDirection,
//                        (byte) (mask | XP | ZN),
//                        allowedTraversalDirections,
//                        visibleTraversalDirections,
//                        sectionVisibilityBits,
//                        inner
//                );
//                // G (southeast x+ z+)
//                tryAdd(
//                        this.sectionTree.getSectionIdx(sectionX + dx, sectionY, sectionZ + dz),
//                        this.sectionTree,
//                        inDirection,
//                        (byte) (mask | XP | ZP),
//                        allowedTraversalDirections,
//                        visibleTraversalDirections,
//                        sectionVisibilityBits,
//                        inner
//                );
//                // H (southwest x- z+)
//                tryAdd(
//                        this.sectionTree.getSectionIdx(sectionX - dx, sectionY, sectionZ + dz),
//                        this.sectionTree,
//                        inDirection,
//                        (byte) (mask | XN | ZP),
//                        allowedTraversalDirections,
//                        visibleTraversalDirections,
//                        sectionVisibilityBits,
//                        inner
//                );
//                // I (northwest x- z-)
//                tryAdd(
//                        this.sectionTree.getSectionIdx(sectionX - dx, sectionY, sectionZ - dz),
//                        this.sectionTree,
//                        inDirection,
//                        (byte) (mask | XN | ZN),
//                        allowedTraversalDirections,
//                        visibleTraversalDirections,
//                        sectionVisibilityBits,
//                        inner
//                );
//            }
//        }
//
//        for (int distance = 1; distance <= this.chunkViewDistance; distance++) {
//            // nodes are checked at the following distances:
//            // 1 2 3 . 3 2 1
//            // 2 3 . . . 3 2
//            // 3 . . . . . 3
//            // . . . . . . .
//            // 3 . . . . . 3
//            // 2 3 . . . 3 2
//            // 1 2 3 . 3 2 1
//
//            IntList outer = sections[2 * this.chunkViewDistance - distance + 1] = new IntArrayList();
//
//            for (int i = 0; i < distance; i++) {
//                int dx = this.chunkViewDistance - i;
//                int dz = this.chunkViewDistance - distance + i + 1;
//
//                // J (northeast x+ z-)
//                tryAdd(
//                        this.sectionTree.getSectionIdx(sectionX + dx, sectionY, sectionZ - dz),
//                        this.sectionTree,
//                        inDirection,
//                        (byte) (mask | XP | ZN),
//                        allowedTraversalDirections,
//                        visibleTraversalDirections,
//                        sectionVisibilityBits,
//                        outer
//                );
//                // K (southeast x+ z+)
//                tryAdd(
//                        this.sectionTree.getSectionIdx(sectionX + dx, sectionY, sectionZ + dz),
//                        this.sectionTree,
//                        inDirection,
//                        (byte) (mask | XP | ZP),
//                        allowedTraversalDirections,
//                        visibleTraversalDirections,
//                        sectionVisibilityBits,
//                        outer
//                );
//                // L (southwest x- z+)
//                tryAdd(
//                        this.sectionTree.getSectionIdx(sectionX - dx, sectionY, sectionZ + dz),
//                        this.sectionTree,
//                        inDirection,
//                        (byte) (mask | XN | ZP),
//                        allowedTraversalDirections,
//                        visibleTraversalDirections,
//                        sectionVisibilityBits,
//                        outer
//                );
//                // M (northwest x- z-)
//                tryAdd(
//                        this.sectionTree.getSectionIdx(sectionX - dx, sectionY, sectionZ - dz),
//                        this.sectionTree,
//                        inDirection,
//                        (byte) (mask | XN | ZN),
//                        allowedTraversalDirections,
//                        visibleTraversalDirections,
//                        sectionVisibilityBits,
//                        outer
//                );
//            }
//        }
//
//        // Trim the section lists (so no empty at the front or back)
//        int first = 0;
//        int last = sections.length - 1;
//
//        while (sections[first].isEmpty()) {
//            if (++first >= sections.length) {
//                return null;
//            }
//        }
//
//        while (sections[last].isEmpty()) {
//            last--;
//        }
//
//        last++;
//
//        if (first != 0 && last != sections.length) {
//            IntList[] trimmed = new IntList[last - first];
//            System.arraycopy(sections, first, trimmed, 0, last - first);
//            return trimmed;
//        } else {
//            return sections;
//        }
//    }
//
//    public int getAdjacent(int id, Direction direction) {
////        if (adjacentSectionIdx != ABSENT_VALUE) {
////            this.sectionAdjacent[(adjacentSectionIdx * DirectionUtil.COUNT) +
////                                 DirectionUtil.getOppositeId(direction.ordinal())] = sectionIdx;
////        }
////
////        this.sectionAdjacent[(sectionIdx * DirectionUtil.COUNT) + direction.ordinal()] = adjacentSectionIdx;
//
//        return this.sectionAdjacent[(id * DirectionUtil.COUNT) + direction];
//    }
    
    public Iterator<RenderSection> getVisibleSectionIterator() {
        return new VisibleSectionIterator();
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
    
    public boolean isSectionVisible(int x, int y, int z) {
        int sectionIdx = this.sectionTree.getSectionIdx(x, y, z);
        
        if (sectionIdx == SectionTree.OUT_OF_BOUNDS_INDEX) {
            return false;
        }
        
        return this.sectionVisibilityBits.get(sectionIdx);
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
    
    public int getVisibilityData(int sectionIdx, int incomingDirection) {
        return this.sectionDirVisibilityData[(sectionIdx * DirectionUtil.COUNT) + incomingDirection];
    }
}