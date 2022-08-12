package net.caffeinemc.sodium.render.chunk.occlusion;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Arrays;
import net.caffeinemc.sodium.interop.vanilla.math.frustum.Frustum;
import net.caffeinemc.sodium.util.DirectionUtil;
import net.caffeinemc.sodium.util.collections.BitArray;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;

public class SectionOcclusion {
    
    private final SectionTree tree;
    
    // Chunks are grouped by manhattan distance to the start chunk, and given
    // the fact that the chunk graph is bipartite, it's possible to simply
    // alternate the lists to form a queue
    private IntList currentQueue;
    private IntList nextQueue;
    
    /*
     * The outgoing directions out of the chunk
     */
    byte[] visibleTraversalDirections;
    /*
     * The directions that the bfs is still allowed to check
     */
    byte[] allowedTraversalDirections;
    
    public SectionOcclusion(SectionTree tree) {
        this.tree = tree;
        
        // TODO: correctly predict size, maybe inline array and keep position?
        this.currentQueue = new IntArrayList(128);
        this.nextQueue = new IntArrayList(128);
        
        this.visibleTraversalDirections = new byte[tree.getSectionTableSize()];
        this.allowedTraversalDirections = new byte[tree.getSectionTableSize()];
    }
    
    public IntArrayList calculateVisibleSections(SectionTree tree, Frustum frustum, BlockPos origin, boolean useOcclusionCulling) {
        // Run the frustum check early so the fallback can use it too
        BitArray nodeVisitable = tree.findVisibleSections(frustum);

        // It's possible to improve culling for spectator mode players in walls,
        // but the time cost to update this algorithm, bugfix it and maintain it
        // on top of the potential slowdown of the normal path seems to outweigh
        // any gains in those rare circumstances
        int visibilityOverride = useOcclusionCulling ? 0 : -1;
        
        IntList[] startingNodes = null;

        int startSectionIdx = tree.getSectionIdx(
                ChunkSectionPos.getSectionCoord(origin.getX()),
                ChunkSectionPos.getSectionCoord(origin.getY()),
                ChunkSectionPos.getSectionCoord(origin.getZ())
        );

        if (startSectionIdx == SectionTree.ABSENT_VALUE) {
            startingNodes = this.getStartingNodesFallback(
                    tree,
                    nodeVisitable,
                    this.allowedTraversalDirections,
                    this.visibleTraversalDirections
            );
        } else {
            this.currentQueue.add(startSectionIdx);

            byte visible = (byte) visibilityOverride;

            for (int direction = 0; direction < DirectionUtil.COUNT; direction++) {
                visible |= tree.getVisibilityData(startSectionIdx, direction);
            }
    
            this.allowedTraversalDirections[startSectionIdx] = (byte) -1;
            this.visibleTraversalDirections[startSectionIdx] = visible;
        }


        // It's possible that due to the near clip plane, the chunk the camera is in,
        // isn't actually visible, however since we still need to use it as a starting
        // point for iterating, we make it always visible, which means we need one extra
        // slot in the array
        int[] visibleSections = new int[nodeVisitable.count() + 1];
        int visibleSectionCount = 0;

        int fallbackIndex = 0;

        while (true) {
            if (startingNodes != null && fallbackIndex < startingNodes.length) {
                this.currentQueue.addAll(startingNodes[fallbackIndex]);
                fallbackIndex++;

                // It's possible that due to unloaded chunks, there entries in
                // the startingNodes array that are empty
            } else if (this.currentQueue.isEmpty()) {
                break;
            }

            for (int i = 0, length = this.currentQueue.size(); i < length; i++) {
                int sectionIdx = this.currentQueue.getInt(i);

                visibleSections[visibleSectionCount++] = sectionIdx;

                byte allowedTraversalDirection = this.allowedTraversalDirections[sectionIdx];
                byte visibleTraversalDirection = this.visibleTraversalDirections[sectionIdx];

                for (int outgoingDirection = 0; outgoingDirection < DirectionUtil.COUNT; outgoingDirection++) {
                    if ((visibleTraversalDirection & (1 << outgoingDirection)) == 0) {
                        continue;
                    }

                    int adjacentNodeId = tree.getAdjacent(sectionIdx, outgoingDirection);

                    if (adjacentNodeId == SectionTree.ABSENT_VALUE || !nodeVisitable.get(adjacentNodeId)) {
                        continue;
                    }

                    int reverseDirection = DirectionUtil.getOppositeId(outgoingDirection);

                    int visible = tree.getVisibilityData(adjacentNodeId, reverseDirection) | visibilityOverride;
                    // prevent further iterations to backtrack
                    int newAllowed = allowedTraversalDirection & ~(1 << reverseDirection);

                    if (this.allowedTraversalDirections[adjacentNodeId] == 0) {
                        this.nextQueue.add(adjacentNodeId);
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
                    this.allowedTraversalDirections[adjacentNodeId] |= newAllowed;
                    this.visibleTraversalDirections[adjacentNodeId] |= newAllowed & visible;
                }
            }

            // swap and reset
            IntList temp = this.currentQueue;
            this.currentQueue = this.nextQueue;
            this.nextQueue = temp;
            this.nextQueue.clear();
        }
        
        Arrays.fill(this.allowedTraversalDirections, (byte) 0);
        Arrays.fill(this.visibleTraversalDirections, (byte) 0);

        return IntArrayList.wrap(visibleSections, visibleSectionCount);
    }

    private static void tryAdd(int sectionIdx, SectionTree tree, int direction, byte directionMask, byte[] allowedTraversalDirections, byte[] visibleTraversalDirections, BitArray visibleSections, IntList sections) {
        if (sectionIdx != SectionTree.ABSENT_VALUE && visibleSections.get(sectionIdx)) {
            sections.add(sectionIdx);

            int visible = tree.getVisibilityData(sectionIdx, direction);

            allowedTraversalDirections[sectionIdx] = directionMask;
            visibleTraversalDirections[sectionIdx] = (byte) (directionMask & visible);
        }
    }

    private static final int XP = 1 << DirectionUtil.X_PLUS;
    private static final int XN = 1 << DirectionUtil.X_MIN;
    private static final int ZP = 1 << DirectionUtil.Z_PLUS;
    private static final int ZN = 1 << DirectionUtil.Z_MIN;

    private IntList[] getStartingNodesFallback(BitArray visible, byte[] allowedTraversalDirections, byte[] visibleTraversalDirections) {
        IntList[] sections = new IntList[this.chunkViewDistance * 2 + 1];
        
        int sectionX = tree.getOriginSectionX();
        int sectionY = tree.getOriginSectionY();
        int sectionZ = tree.getOriginSectionZ();

        int direction = sectionY < this.heightLimitView.getBottomSectionCoord() ? Direction.UP.getId() : Direction.DOWN.getId();
        int inDirection = DirectionUtil.getOppositeId(direction);
        // in theory useless
        int mask = 1 << direction;

        // M M M B J J J
        // M M I B F J J
        // M I I B F F J
        // E E E A C C C
        // L H H D G G K
        // L L H D G K K
        // L L L D K K K

        // A
        tryAdd(
                tree.getSectionIdx(sectionX, sectionY, sectionZ),
                tree,
                inDirection,
                (byte) -1,
                allowedTraversalDirections,
                visibleTraversalDirections,
                visible,
                sections[0] = new IntArrayList(1)
        );

        for (int distance = 1; distance <= this.chunkViewDistance; distance++) {
            IntList inner = sections[distance] = new IntArrayList();

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
                tryAdd(
                        tree.getSectionIdx(sectionX, sectionY, sectionZ - distance),
                        tree,
                        inDirection,
                        (byte) (mask | XN | ZN | XP),
                        allowedTraversalDirections,
                        visibleTraversalDirections,
                        visible,
                        inner
                );
                // C (east x+)
                tryAdd(
                        tree.getSectionIdx(sectionX + distance, sectionY, sectionZ),
                        tree,
                        inDirection,
                        (byte) (mask | XP | ZN | ZP),
                        allowedTraversalDirections,
                        visibleTraversalDirections,
                        visible,
                        inner
                );
                // D (south z+)
                tryAdd(
                        tree.getSectionIdx(sectionX, sectionY, sectionZ + distance),
                        tree,
                        inDirection,
                        (byte) (mask | XP | ZP | XN),
                        allowedTraversalDirections,
                        visibleTraversalDirections,
                        visible,
                        inner
                );
                // E (west x-)
                tryAdd(
                        tree.getSectionIdx(sectionX - distance, sectionY, sectionZ),
                        tree,
                        inDirection,
                        (byte) (mask | XN | ZP | ZN),
                        allowedTraversalDirections,
                        visibleTraversalDirections,
                        visible,
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
                tryAdd(
                        tree.getSectionIdx(sectionX + dx, sectionY, sectionZ - dz),
                        tree,
                        inDirection,
                        (byte) (mask | XP | ZN),
                        allowedTraversalDirections,
                        visibleTraversalDirections,
                        visible,
                        inner
                );
                // G (southeast x+ z+)
                tryAdd(
                        tree.getSectionIdx(sectionX + dx, sectionY, sectionZ + dz),
                        tree,
                        inDirection,
                        (byte) (mask | XP | ZP),
                        allowedTraversalDirections,
                        visibleTraversalDirections,
                        visible,
                        inner
                );
                // H (southwest x- z+)
                tryAdd(
                        tree.getSectionIdx(sectionX - dx, sectionY, sectionZ + dz),
                        tree,
                        inDirection,
                        (byte) (mask | XN | ZP),
                        allowedTraversalDirections,
                        visibleTraversalDirections,
                        visible,
                        inner
                );
                // I (northwest x- z-)
                tryAdd(
                        tree.getSectionIdx(sectionX - dx, sectionY, sectionZ - dz),
                        tree,
                        inDirection,
                        (byte) (mask | XN | ZN),
                        allowedTraversalDirections,
                        visibleTraversalDirections,
                        visible,
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

            IntList outer = sections[2 * this.chunkViewDistance - distance + 1] = new IntArrayList();

            for (int i = 0; i < distance; i++) {
                int dx = this.chunkViewDistance - i;
                int dz = this.chunkViewDistance - distance + i + 1;

                // J (northeast x+ z-)
                tryAdd(
                        tree.getSectionIdx(sectionX + dx, sectionY, sectionZ - dz),
                        tree,
                        inDirection,
                        (byte) (mask | XP | ZN),
                        allowedTraversalDirections,
                        visibleTraversalDirections,
                        visible,
                        outer
                );
                // K (southeast x+ z+)
                tryAdd(
                        tree.getSectionIdx(sectionX + dx, sectionY, sectionZ + dz),
                        tree,
                        inDirection,
                        (byte) (mask | XP | ZP),
                        allowedTraversalDirections,
                        visibleTraversalDirections,
                        visible,
                        outer
                );
                // L (southwest x- z+)
                tryAdd(
                        tree.getSectionIdx(sectionX - dx, sectionY, sectionZ + dz),
                        tree,
                        inDirection,
                        (byte) (mask | XN | ZP),
                        allowedTraversalDirections,
                        visibleTraversalDirections,
                        visible,
                        outer
                );
                // M (northwest x- z-)
                tryAdd(
                        tree.getSectionIdx(sectionX - dx, sectionY, sectionZ - dz),
                        tree,
                        inDirection,
                        (byte) (mask | XN | ZN),
                        allowedTraversalDirections,
                        visibleTraversalDirections,
                        visible,
                        outer
                );
            }
        }

        // Trim the section lists (so no empty at the front or back)
        int first = 0;
        int last = sections.length - 1;

        while (sections[first].isEmpty()) {
            if (++first >= sections.length) {
                return null;
            }
        }

        while (sections[last].isEmpty()) {
            last--;
        }

        last++;

        if (first != 0 && last != sections.length) {
            IntList[] trimmed = new IntList[last - first];
            System.arraycopy(sections, first, trimmed, 0, last - first);
            return trimmed;
        } else {
            return sections;
        }
    }
}