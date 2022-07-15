package net.caffeinemc.sodium.render.chunk.occlusion;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntIterator;
import net.caffeinemc.sodium.interop.vanilla.math.frustum.Frustum;
import net.caffeinemc.sodium.render.chunk.RenderSection;
import net.caffeinemc.sodium.util.DirectionUtil;
import net.caffeinemc.sodium.util.collections.BitArray;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class ChunkOcclusion {
    public static IntArrayList calculateVisibleSections(ChunkTree tree, Frustum frustum, World world, BlockPos origin,
                                                        int chunkViewDistance, boolean useOcclusionCulling) {
        BitArray nodeVisitable = tree.findVisibleSections(frustum);
        byte[] allowedTraversalDirections = new byte[nodeVisitable.capacity()];
        byte[] visibleTraversalDirections = new byte[nodeVisitable.capacity()];

        int visibilityOverride = useOcclusionCulling ? 0 : -1;

        IntArrayFIFOQueue queue = new IntArrayFIFOQueue(256);
        IntArrayList startingNodes = ChunkOcclusion.getStartingNodes(tree, frustum, world, origin, chunkViewDistance);

        for (IntIterator it = startingNodes.intIterator(); it.hasNext(); ) {
            int sectionId = it.nextInt();
            queue.enqueue(sectionId);

            byte visible = (byte)visibilityOverride;

            for (int direction = 0; direction < DirectionUtil.COUNT; direction++) {
                visible |= tree.getVisibilityData(sectionId, direction);
            }

            allowedTraversalDirections[sectionId] = -1;
            visibleTraversalDirections[sectionId] = visible;
        }


        int[] visibleSections = new int[nodeVisitable.count() + startingNodes.size()];
        int visibleSectionCount = 0;


        while (!queue.isEmpty()) {
            int sectionId = queue.dequeueInt();

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
                byte oldAllowed = allowedTraversalDirections[adjacentNodeId];
                int newAllowed = allowedTraversalDirection & ~(1 << reverseDirection);

                if (oldAllowed == 0) {
                    queue.enqueue(adjacentNodeId);
                    if (newAllowed == 0) {
                        // avoid adding it twice to the list!
                        newAllowed = 0b1000_0000; // not 0 but doesn't set any of the direction bits
                    }
                }

                allowedTraversalDirections[adjacentNodeId] = (byte) (oldAllowed | newAllowed);
                visibleTraversalDirections[adjacentNodeId] |= (byte) (newAllowed & visible);
            }
        }

        return IntArrayList.wrap(visibleSections, visibleSectionCount);
    }

    public static IntArrayList getStartingNodes(ChunkTree tree, Frustum frustum, World world, BlockPos origin, int renderDistance) {
        RenderSection section = tree.getSection(ChunkSectionPos.fromBlockPos(origin.asLong()));

        if (section == null) {
            return getStartingNodesFallback(tree, frustum, world, origin, renderDistance);
        }

        return IntArrayList.of(section.id());
    }

    private static IntArrayList getStartingNodesFallback(ChunkTree tree, Frustum frustum, World world, BlockPos origin, int renderDistance) {
        int estimatedCapacity = MathHelper.square(renderDistance * 2);
        int count = 0;

        IntArrayList sections = new IntArrayList(estimatedCapacity);
        FloatArrayList distances = new FloatArrayList(estimatedCapacity);

        ChunkSectionPos chunkPos = ChunkSectionPos.from(origin);
        int chunkX = chunkPos.getX();
        int chunkY = MathHelper.clamp(chunkPos.getY(), world.getBottomSectionCoord(), world.getTopSectionCoord() - 1);
        int chunkZ = chunkPos.getZ();

        final double originX = origin.getX();
        final double originZ = origin.getZ();

        for (int x2 = -renderDistance; x2 <= renderDistance; ++x2) {
            for (int z2 = -renderDistance; z2 <= renderDistance; ++z2) {
                RenderSection node = tree.getSection(chunkX + x2, chunkY, chunkZ + z2);

                if (node != null && node.isWithinFrustum(frustum)) {
                    sections.add(node.id());
                    distances.add((float) node.getDistance(originX, originZ));
                    count++;
                }
            }
        }

        int[] sortedIndices = new int[count];

        for (int i = 0; i < count; i++) {
            sortedIndices[i] = i;
        }

        IntArrays.mergeSort(sortedIndices, (k1, k2) -> Float.compare(distances.getFloat(k1), distances.getFloat(k2)));

        int[] sortedSections = new int[count];

        for (int i = 0; i < count; i++) {
            sortedSections[i] = sections.getInt(sortedIndices[i]);
        }

        return IntArrayList.wrap(sortedSections, count);
    }

    private static int markDirection(int state, int dir) {
        return (state | (1 << dir));
    }

    private static boolean hasDirection(int state, int dir) {
        return (state & (1 << dir)) != 0;
    }
}