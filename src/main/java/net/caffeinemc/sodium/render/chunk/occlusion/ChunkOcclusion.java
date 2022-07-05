package net.caffeinemc.sodium.render.chunk.occlusion;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntIterator;
import net.caffeinemc.sodium.interop.vanilla.math.frustum.Frustum;
import net.caffeinemc.sodium.render.chunk.RenderSection;
import net.caffeinemc.sodium.render.chunk.state.ChunkGraphIterationQueue;
import net.caffeinemc.sodium.util.DirectionUtil;
import net.caffeinemc.sodium.util.collections.BitArray;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class ChunkOcclusion {
    public static IntArrayList calculateVisibleSections(ChunkTree tree, Frustum frustum, World world, BlockPos origin,
                                                        int chunkViewDistance, boolean useOcclusionCulling) {
        ChunkGraphIterationQueue queue = new ChunkGraphIterationQueue();
        IntArrayList startingNodes = ChunkOcclusion.getStartingNodes(tree, frustum, world, origin, chunkViewDistance);

        for (IntIterator it = startingNodes.intIterator(); it.hasNext(); ) {
            queue.enqueue(it.nextInt(), -1);
        }
    
        BitArray nodeVisitable = tree.findVisibleSections(frustum);
        byte[] nodeState = new byte[nodeVisitable.capacity()];
    
        int[] visibleSections = new int[nodeVisitable.count() + startingNodes.size()];
        int visibleSectionCount = 0;
    
        int visibilityDataMask = useOcclusionCulling ? Integer.MAX_VALUE : 0;

        while (!queue.isEmpty()) {
            int index = queue.dequeIndex();
    
            int sectionId = queue.getSectionId(index);
            int sectionIncomingDirection = queue.getDirection(index);

            visibleSections[visibleSectionCount++] = sectionId;

            int visibilityData;

            if (sectionIncomingDirection != -1) {
                visibilityData = tree.getVisibilityData(sectionId, sectionIncomingDirection);
            } else {
                visibilityData = 0;
            }
    
            byte traversalState = nodeState[sectionId];
            int cullState = traversalState | (visibilityData & visibilityDataMask);

            for (int outgoingDirection = 0; outgoingDirection < DirectionUtil.COUNT; outgoingDirection++) {
                if (hasDirection(cullState, outgoingDirection)) {
                    continue;
                }

                int adjacentNodeId = tree.getAdjacent(sectionId, outgoingDirection);

                if (adjacentNodeId == ChunkTree.ABSENT_VALUE || !nodeVisitable.getAndUnset(adjacentNodeId)) {
                    continue;
                }
    
                int reverseDirection = DirectionUtil.getOppositeId(outgoingDirection);
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
        int estimatedCapacity = MathHelper.square(renderDistance * 2);
        int count = 0;
    
        IntArrayList sections = new IntArrayList(estimatedCapacity);
        FloatArrayList distances = new FloatArrayList(estimatedCapacity);
    
        ChunkSectionPos chunkPos = ChunkSectionPos.from(origin);
        int chunkX = chunkPos.getX();
        int chunkY = MathHelper.clamp(chunkPos.getY(), world.getBottomSectionCoord(), world.getTopSectionCoord() - 1);
        int chunkZ = chunkPos.getZ();

        final float originX = origin.getX();
        final float originZ = origin.getZ();

        for (int x2 = -renderDistance; x2 <= renderDistance; ++x2) {
            for (int z2 = -renderDistance; z2 <= renderDistance; ++z2) {
                RenderSection node = tree.getSection(chunkX + x2, chunkY, chunkZ + z2);

                if (node != null && node.isWithinFrustum(frustum)) {
                    sections.add(node.id());
                    distances.add(node.getDistance(originX, originZ));
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