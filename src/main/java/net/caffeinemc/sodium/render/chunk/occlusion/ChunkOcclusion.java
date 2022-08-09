package net.caffeinemc.sodium.render.chunk.occlusion;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.caffeinemc.sodium.interop.vanilla.math.frustum.Frustum;
import net.caffeinemc.sodium.util.DirectionUtil;
import net.caffeinemc.sodium.util.collections.BitArray;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class ChunkOcclusion {
    private static IntList currentQueue = new IntArrayList(128);
    private static IntList nextQueue = new IntArrayList(128);
    private static int[] visibleSections = new int[0];
    public static IntArrayList calculateVisibleSections(ChunkTree tree, Frustum frustum, World world, BlockPos origin,
                                                        int chunkViewDistance, boolean useOcclusionCulling) {
        // Run the frustum check early so the fallback can use it too
        BitArray nodeVisitable = tree.findVisibleSections(frustum);

        // visible: the outgoing directions out of the chunk
        // allowed: the directions that the bfs is still allowed to check
        byte[] allowedTraversalDirections = new byte[nodeVisitable.capacity()];
        byte[] visibleTraversalDirections = new byte[nodeVisitable.capacity()];

        // It's possible to improve culling for spectator mode players in walls,
        // but the time cost to update this algorithm, bugfix it and maintain it
        // on top of the potential slowdown of the normal path seems to outweigh
        // any gains in those rare circumstances
        int visibilityOverride = useOcclusionCulling ? 0 : -1;

        // Chunks are grouped by manhattan distance to the start chunk, and given
        // the fact that the chunk graph is bipartite, it's possible to simply
        // alternate the lists to form a queue
        IntList[] startingNodes = null;

        int startSectionId = tree.getSectionId(ChunkSectionPos.toLong(origin));

        if (startSectionId == ChunkTree.ABSENT_VALUE) {
            startingNodes = getStartingNodesFallback(tree, world, origin, chunkViewDistance, nodeVisitable, allowedTraversalDirections, visibleTraversalDirections);
        } else {
            currentQueue.add(startSectionId);

            byte visible = (byte) visibilityOverride;

            for (int direction = 0; direction < DirectionUtil.COUNT; direction++) {
                visible |= tree.getVisibilityData(startSectionId, direction);
            }

            allowedTraversalDirections[startSectionId] = (byte) -1;
            visibleTraversalDirections[startSectionId] = visible;
        }


        // It's possible that due to the near clip plane, the chunk the camera is in,
        // isn't actually visible, however since we still need to use it as a starting
        // point for iterating, we make it always visible, which means we need one extra
        // slot in the array
        if (visibleSections.length <= nodeVisitable.count() + 1) {
            visibleSections = new int[nodeVisitable.count() + 1];
        }
        int visibleSectionCount = 0;

        int fallbackIndex = 0;
        while (true) {
            if (startingNodes != null && fallbackIndex < startingNodes.length) {
                currentQueue.addAll(startingNodes[fallbackIndex]);
                fallbackIndex++;

                // It's possible that due to unloaded chunks, there entries in
                // the startingNodes array that are empty
            } else if (currentQueue.isEmpty()) {
                break;
            }

            for (int i = 0, length = currentQueue.size(); i < length; i++) {
                int sectionId = currentQueue.getInt(i);

                visibleSections[visibleSectionCount++] = sectionId;

                byte allowedTraversalDirection = allowedTraversalDirections[sectionId];
                byte visibleTraversalDirection = visibleTraversalDirections[sectionId];

                for (int outgoingDirection = 0; outgoingDirection < DirectionUtil.COUNT; outgoingDirection++) {
                    if ((visibleTraversalDirection & (1 << outgoingDirection)) == 0) {
                        continue;
                    }

                    int adjacentNodeId = tree.getAdjacent(sectionId, outgoingDirection);

                    if (adjacentNodeId == ChunkTree.ABSENT_VALUE || !nodeVisitable.get(adjacentNodeId)) {
                        continue;
                    }

                    int reverseDirection = DirectionUtil.getOppositeId(outgoingDirection);

                    int visible = tree.getVisibilityData(adjacentNodeId, reverseDirection) | visibilityOverride;
                    // prevent further iterations to backtrack
                    int newAllowed = allowedTraversalDirection & ~(1 << reverseDirection);

                    if (allowedTraversalDirections[adjacentNodeId] == 0) {
                        nextQueue.add(adjacentNodeId);
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
                    allowedTraversalDirections[adjacentNodeId] |= newAllowed;
                    visibleTraversalDirections[adjacentNodeId] |= newAllowed & visible;
                }
            }

            // swap and reset
            IntList temp = currentQueue;
            currentQueue = nextQueue;
            nextQueue = temp;
            nextQueue.clear();
        }

        return IntArrayList.wrap(visibleSections, visibleSectionCount);
    }

    private static void tryAdd(int sectionId, ChunkTree tree, int direction, byte directionMask, byte[] allowedTraversalDirections, byte[] visibleTraversalDirections, BitArray visibleSections, IntList sections) {
        if (sectionId != ChunkTree.ABSENT_VALUE && visibleSections.get(sectionId)) {
            sections.add(sectionId);

            int visible = tree.getVisibilityData(sectionId, direction);

            allowedTraversalDirections[sectionId] = directionMask;
            visibleTraversalDirections[sectionId] = (byte) (directionMask & visible);
        }
    }

    private static final int XP = 1 << DirectionUtil.X_PLUS;
    private static final int XN = 1 << DirectionUtil.X_MIN;
    private static final int ZP = 1 << DirectionUtil.Z_PLUS;
    private static final int ZN = 1 << DirectionUtil.Z_MIN;

    private static IntList[] getStartingNodesFallback(ChunkTree tree, World world, BlockPos origin, int renderDistance, BitArray visible, byte[] allowedTraversalDirections, byte[] visibleTraversalDirections) {
        IntList[] sections = new IntList[renderDistance * 2 + 1];

        ChunkSectionPos chunkPos = ChunkSectionPos.from(origin);
        int chunkX = chunkPos.getX();
        int chunkY = MathHelper.clamp(chunkPos.getY(), world.getBottomSectionCoord(), world.getTopSectionCoord() - 1);
        int chunkZ = chunkPos.getZ();

        int direction = chunkPos.getY() < world.getBottomSectionCoord() ? Direction.UP.getId() : Direction.DOWN.getId();
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
        tryAdd(tree.getSectionId(chunkX, chunkY, chunkZ), tree, inDirection, (byte) -1,
                allowedTraversalDirections, visibleTraversalDirections, visible, sections[0] = new IntArrayList(1));

        for (int distance = 1; distance <= renderDistance; distance++) {
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
                tryAdd(tree.getSectionId(chunkX, chunkY, chunkZ - distance), tree, inDirection, (byte) (mask | XN | ZN | XP), allowedTraversalDirections, visibleTraversalDirections, visible, inner);
                // C (east x+)
                tryAdd(tree.getSectionId(chunkX + distance, chunkY, chunkZ), tree, inDirection, (byte) (mask | XP | ZN | ZP), allowedTraversalDirections, visibleTraversalDirections, visible, inner);
                // D (south z+)
                tryAdd(tree.getSectionId(chunkX, chunkY, chunkZ + distance), tree, inDirection, (byte) (mask | XP | ZP | XN), allowedTraversalDirections, visibleTraversalDirections, visible, inner);
                // E (west x-)
                tryAdd(tree.getSectionId(chunkX - distance, chunkY, chunkZ), tree, inDirection, (byte) (mask | XN | ZP | ZN), allowedTraversalDirections, visibleTraversalDirections, visible, inner);
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
                tryAdd(tree.getSectionId(chunkX + dx, chunkY, chunkZ - dz), tree, inDirection, (byte) (mask | XP | ZN), allowedTraversalDirections, visibleTraversalDirections, visible, inner);
                // G (southeast x+ z+)
                tryAdd(tree.getSectionId(chunkX + dx, chunkY, chunkZ + dz), tree, inDirection, (byte) (mask | XP | ZP), allowedTraversalDirections, visibleTraversalDirections, visible, inner);
                // H (southwest x- z+)
                tryAdd(tree.getSectionId(chunkX - dx, chunkY, chunkZ + dz), tree, inDirection, (byte) (mask | XN | ZP), allowedTraversalDirections, visibleTraversalDirections, visible, inner);
                // I (northwest x- z-)
                tryAdd(tree.getSectionId(chunkX - dx, chunkY, chunkZ - dz), tree, inDirection, (byte) (mask | XN | ZN), allowedTraversalDirections, visibleTraversalDirections, visible, inner);
            }
        }

        for (int distance = 1; distance <= renderDistance; distance++) {
            // nodes are checked at the following distances:
            // 1 2 3 . 3 2 1
            // 2 3 . . . 3 2
            // 3 . . . . . 3
            // . . . . . . .
            // 3 . . . . . 3
            // 2 3 . . . 3 2
            // 1 2 3 . 3 2 1

            IntList outer = sections[2 * renderDistance - distance + 1] = new IntArrayList();

            for (int i = 0; i < distance; i++) {
                int dx = renderDistance - i;
                int dz = renderDistance - distance + i + 1;

                // J (northeast x+ z-)
                tryAdd(tree.getSectionId(chunkX + dx, chunkY, chunkZ - dz), tree, inDirection, (byte) (mask | XP | ZN), allowedTraversalDirections, visibleTraversalDirections, visible, outer);
                // K (southeast x+ z+)
                tryAdd(tree.getSectionId(chunkX + dx, chunkY, chunkZ + dz), tree, inDirection, (byte) (mask | XP | ZP), allowedTraversalDirections, visibleTraversalDirections, visible, outer);
                // L (southwest x- z+)
                tryAdd(tree.getSectionId(chunkX - dx, chunkY, chunkZ + dz), tree, inDirection, (byte) (mask | XN | ZP), allowedTraversalDirections, visibleTraversalDirections, visible, outer);
                // M (northwest x- z-)
                tryAdd(tree.getSectionId(chunkX - dx, chunkY, chunkZ - dz), tree, inDirection, (byte) (mask | XN | ZN), allowedTraversalDirections, visibleTraversalDirections, visible, outer);
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