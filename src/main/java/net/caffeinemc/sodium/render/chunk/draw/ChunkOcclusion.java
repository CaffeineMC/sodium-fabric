package net.caffeinemc.sodium.render.chunk.draw;

import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceList;
import net.caffeinemc.sodium.interop.vanilla.math.frustum.Frustum;
import net.caffeinemc.sodium.render.chunk.RenderSection;
import net.caffeinemc.sodium.render.chunk.state.ChunkGraphIterationQueue;
import net.caffeinemc.sodium.util.DirectionUtil;
import net.caffeinemc.sodium.util.collections.BitArray;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.Comparator;
import java.util.function.ToDoubleFunction;

public class ChunkOcclusion {
    public record Results(BitArray visibilityTable, ReferenceList<RenderSection> visibleList) {

    }

    public static Results calculateVisibleSections(ChunkTree tree, ChunkCameraContext camera, Frustum frustum,
                                                   World world, int chunkViewDistance, boolean spectator) {
        var queue = new ChunkGraphIterationQueue();
        var blockOrigin = new BlockPos(camera.blockX, camera.blockY, camera.blockZ);

        var sectionCount = tree.getSectionTableSize();

        var frustumTable = new BitArray(sectionCount);
        var visitedTable = new BitArray(sectionCount);

        var traversalTable = new byte[sectionCount];

        tree.calculateVisible(frustum, frustumTable);

        boolean useOcclusionCulling = true;

        if (initSearch(tree, queue, blockOrigin)) {
            if (spectator && world.getBlockState(blockOrigin).isOpaqueFullCube(world, blockOrigin)) {
                useOcclusionCulling = false;
            }
        } else {
            initSearchFallback(tree, queue, frustumTable, world, blockOrigin, chunkViewDistance);
        }

        var drawDistance = MathHelper.square((chunkViewDistance + 1) * 16.0f);
        var visibleList = new ReferenceArrayList<RenderSection>(2048);

        for (int i = 0; i < queue.size(); i++) {
            var sectionId = queue.getRender(i);
            var incomingDirection = queue.getDirection(i);

            var node = tree.getNodeById(sectionId);
            var section = tree.getSectionForNode(sectionId);

            if (section.getDistance(camera.posX, camera.posZ) > drawDistance) {
                continue;
            }

            visibleList.add(section);

            for (int outgoingDirection : DirectionUtil.ALL_DIRECTION_IDS) {
                var parentTraversalState = traversalTable[sectionId];

                // The origin node has already been traversed in this direction
                if (canTraverse(parentTraversalState, outgoingDirection)) {
                    continue;
                }

                // The adjacent chunk is not visible through the iterated node
                if (useOcclusionCulling && (incomingDirection != -1 && !node.isVisibleThrough(incomingDirection, outgoingDirection))) {
                    continue;
                }

                int adjacentSectionId = tree.getAdjacent(sectionId, outgoingDirection);

                // The adjacent chunk isn't loaded
                if (adjacentSectionId == -1) {
                    continue;
                }

                // The adjacent chunk has already been visited
                if (visitedTable.getAndSet(adjacentSectionId)) {
                    continue;
                }

                // The adjacent chunk isn't within the frustum
                if (!frustumTable.get(adjacentSectionId)) {
                    continue;
                }

                var reverseDirection = DirectionUtil.getOppositeId(outgoingDirection);
                traversalTable[adjacentSectionId] = (byte) updateTraversalState(parentTraversalState, reverseDirection);

                queue.add(adjacentSectionId, reverseDirection);
            }
        }

        return new Results(frustumTable, visibleList);
    }

    private static boolean initSearch(ChunkTree tree, ChunkGraphIterationQueue queue, BlockPos origin) {
        RenderSection section = tree.getSection(ChunkSectionPos.fromBlockPos(origin.asLong()));

        if (section == null) {
            return false;
        }

        queue.add(section.id(), -1);

        return true;
    }

    private static void initSearchFallback(ChunkTree tree, ChunkGraphIterationQueue queue, BitArray frustumTable, World world, BlockPos origin, int renderDistance) {
        var sorted = new ReferenceArrayList<RenderSection>();

        var chunkPos = ChunkSectionPos.from(origin);
        var chunkX = chunkPos.getX();
        var chunkY = MathHelper.clamp(chunkPos.getY(), world.getBottomSectionCoord(), world.getTopSectionCoord() - 1);
        var chunkZ = chunkPos.getZ();

        for (int x2 = -renderDistance; x2 <= renderDistance; ++x2) {
            for (int z2 = -renderDistance; z2 <= renderDistance; ++z2) {
                var node = tree.getSection(chunkX + x2, chunkY, chunkZ + z2);

                if (node != null && frustumTable.get(node.id())) {
                    sorted.add(node);
                }
            }
        }

        sorted.sort(Comparator.comparingDouble(new ToDoubleFunction<>() {
            final float x = origin.getX();
            final float z = origin.getZ();

            @Override
            public double applyAsDouble(RenderSection node) {
                // All chunks will be at the same Y-level, so only compare the XZ-coordinates
                return node.getDistance(this.x, this.z);
            }
        }));

        for (RenderSection render : sorted) {
            queue.add(render.id(), -1);
        }
    }

    private static int updateTraversalState(byte parent, int dir) {
        return (parent | (1 << dir));
    }

    private static boolean canTraverse(byte state, int dir) {
        return (state & 1 << dir) != 0;
    }

}