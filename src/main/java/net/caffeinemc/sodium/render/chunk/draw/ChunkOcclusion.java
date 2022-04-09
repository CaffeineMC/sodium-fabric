package net.caffeinemc.sodium.render.chunk.draw;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.caffeinemc.sodium.interop.vanilla.math.frustum.Frustum;
import net.caffeinemc.sodium.render.chunk.RenderSection;
import net.caffeinemc.sodium.render.chunk.state.ChunkGraphIterationQueue;
import net.caffeinemc.sodium.util.DirectionUtil;
import net.caffeinemc.sodium.util.collections.BitArray;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.Arrays;
import java.util.Comparator;
import java.util.function.ToDoubleFunction;

public class ChunkOcclusion {
    public static IntArrayList calculateVisibleSections(ChunkTree tree, Frustum frustum, World world, BlockPos origin,
                                                        int chunkViewDistance, boolean useOcclusionCulling) {
        var queue = new ChunkGraphIterationQueue();
        var startingNodes = ChunkOcclusion.getStartingNodes(tree, frustum, world, origin, chunkViewDistance);

        for (var it = startingNodes.intIterator(); it.hasNext(); ) {
            queue.enqueue(it.nextInt(), -1);
        }

        var nodeVisitable = tree.findVisibleSections(frustum);
        var nodeState = new byte[nodeVisitable.capacity()];

        var visibleSections = new int[nodeVisitable.count() + startingNodes.size()];
        var visibleSectionCount = 0;

        var visibilityDataMask = useOcclusionCulling ? Integer.MAX_VALUE : 0;

        while (!queue.isEmpty()) {
            var index = queue.dequeIndex();

            var sectionId = queue.getSectionId(index);
            var sectionIncomingDirection = queue.getDirection(index);

            visibleSections[visibleSectionCount++] = sectionId;

            int visibilityData;

            if (sectionIncomingDirection != -1) {
                visibilityData = tree.getVisibilityData(sectionId, sectionIncomingDirection);
            } else {
                visibilityData = 0;
            }

            var traversalState = nodeState[sectionId];
            var cullState = traversalState | (visibilityData & visibilityDataMask);

            for (int outgoingDirection = 0; outgoingDirection < DirectionUtil.COUNT; outgoingDirection++) {
                if (hasDirection(cullState, outgoingDirection)) {
                    continue;
                }

                int adjacentNodeId = tree.getAdjacent(sectionId, outgoingDirection);

                if (adjacentNodeId == ChunkTree.ABSENT_VALUE || !nodeVisitable.getAndUnset(adjacentNodeId)) {
                    continue;
                }

                var reverseDirection = DirectionUtil.getOppositeId(outgoingDirection);
                nodeState[adjacentNodeId] = (byte) markDirection(traversalState, reverseDirection);

                queue.enqueue(adjacentNodeId, reverseDirection);
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
        var capacity = MathHelper.square(renderDistance * 2);
        var count = 0;

        var sections = new int[capacity];
        var distances = new float[capacity];

        var chunkPos = ChunkSectionPos.from(origin);
        var chunkX = chunkPos.getX();
        var chunkY = MathHelper.clamp(chunkPos.getY(), world.getBottomSectionCoord(), world.getTopSectionCoord() - 1);
        var chunkZ = chunkPos.getZ();

        final float originX = origin.getX();
        final float originZ = origin.getZ();

        for (int x2 = -renderDistance; x2 <= renderDistance; ++x2) {
            for (int z2 = -renderDistance; z2 <= renderDistance; ++z2) {
                var node = tree.getSection(chunkX + x2, chunkY, chunkZ + z2);

                if (node != null && node.isWithinFrustum(frustum)) {
                    var index = count++;
                    sections[index] = node.id();
                    distances[index] = node.getDistance(originX, originZ);
                }
            }
        }

        var sortedIndices = new int[count];

        for (int i = 0; i < count; i++) {
            sortedIndices[i] = i;
        }

        IntArrays.mergeSort(sortedIndices, (k1, k2) -> Float.compare(distances[k1], distances[k2]));

        var sortedSections = new int[count];

        for (int i = 0; i < count; i++) {
            sortedSections[i] = sections[sortedIndices[i]];
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