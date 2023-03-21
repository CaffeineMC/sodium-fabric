package me.jellysquid.mods.sodium.client.render.chunk.graph;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import me.jellysquid.mods.sodium.client.render.chunk.lists.RegionRenderLists;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegionManager;
import me.jellysquid.mods.sodium.client.util.frustum.Frustum;
import me.jellysquid.mods.sodium.client.util.memory.HeapArena;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.util.ArrayDeque;

public final class GraphSearch {
    private final GraphSearchPool pool;
    private final Frustum frustum;

    private final boolean useOcclusionCulling;

    private final int midX, midY, midZ; // the center of the graph search

    private final Long2LongOpenHashMap searchLists = new Long2LongOpenHashMap();
    private final ArrayDeque<RenderRegion> searchQueue = new ArrayDeque<>();

    private final RenderRegionManager regions;

    private final int renderDistance;

    public GraphSearch(GraphSearchPool pool, RenderRegionManager regions, Camera camera, Frustum frustum, int renderDistance, boolean useOcclusionCulling) {
        this.pool = pool;
        this.regions = regions;

        BlockPos cameraBlockPos = camera.getBlockPos();

        this.midX = cameraBlockPos.getX() >> 4;
        this.midY = cameraBlockPos.getY() >> 4;
        this.midZ = cameraBlockPos.getZ() >> 4;

        this.frustum = frustum;

        this.useOcclusionCulling = useOcclusionCulling;
        this.renderDistance = renderDistance;

        this.searchLists.defaultReturnValue(MemoryUtil.NULL);
    }

    private static boolean isVisible(Frustum frustum, int chunkX, int chunkY, int chunkZ) {
        double posX = chunkX << 4;
        double posY = chunkY << 4;
        double posZ = chunkZ << 4;

        return frustum.testBox(posX, posY, posZ, posX + 16.0D, posY + 16.0D, posZ + 16.0D);
    }

    public ChunkRenderList getVisible() {
        this.enqueueNodes(this.pool, LongArrayList.of(ChunkSectionPos.asLong(this.midX, this.midY, this.midZ)));

        var nonEmptyRenderLists = new ObjectArrayList<RegionRenderLists>();

        RenderRegion region;

        while ((region = this.searchQueue.poll()) != null) {
            if (!region.testVisibility(this.frustum)) {
                continue;
            }

            long pSearchList = this.searchLists.get(region.key());
            RegionRenderLists renderLists;

            try (MemoryStack stack = MemoryStack.stackPush()) {
                long pAdjacentSearchLists = SearchQueueAccessUnsafe.allocateStack(stack);

                for (int direction = 0; direction < GraphDirection.COUNT; direction++) {
                    SearchQueueAccessUnsafe.setAdjacent(pAdjacentSearchLists, direction,
                            this.getNeighborSearchList(this.pool, region, direction));
                }

                SearchQueueAccessUnsafe.setSelf(pAdjacentSearchLists, pSearchList);

                renderLists = this.processQueue(region, pSearchList, pAdjacentSearchLists);
            }

            if (!renderLists.isEmpty()) {
                nonEmptyRenderLists.add(renderLists);
            } else {
                this.pool.releaseRenderList(renderLists);
            }
        }

        this.pool.releaseSearchLists();

        this.searchLists.clear();
        this.searchQueue.clear();

        return new ChunkRenderList(nonEmptyRenderLists);
    }

    public long getNeighborSearchList(GraphSearchPool pool, RenderRegion region, int direction) {
        long pos = ChunkSectionPos.asLong(
                region.getX() + GraphDirection.x(direction),
                region.getY() + GraphDirection.y(direction),
                region.getZ() + GraphDirection.z(direction)
        );

        long searchList = this.searchLists.get(pos);

        if (searchList == MemoryUtil.NULL) {
            searchList = this.createSearchList(pool, pos);
        }

        return searchList;
    }

    private long createSearchList(GraphSearchPool pool, long pos) {
        var list = pool.createSearchList();

        this.searchLists.put(pos, list);

        var region = this.regions.getByKey(pos);

        if (region != null) {
            this.searchQueue.add(region);
        }

        return list;
    }

    public void enqueueNodes(GraphSearchPool pool, LongArrayList nodes) {
        LongIterator it = nodes.longIterator();

        while (it.hasNext()) {
            long pos = it.nextLong();

            int x = ChunkSectionPos.unpackX(pos);
            int y = ChunkSectionPos.unpackY(pos);
            int z = ChunkSectionPos.unpackZ(pos);

            var section = this.regions.getSection(x, y, z);

            if (section == null) {
                continue;
            }

            var region = section.getRegion();

            var pList = pool.createSearchList();
            SearchQueueUnsafe.enqueue(pList, x, y, z);

            this.searchLists.put(region.key(), pList);
            this.searchQueue.add(region);
        }
    }

    private RegionRenderLists processQueue(RenderRegion region, long pSelf, long pAdjacent) {
        var renderLists = this.pool.createRenderLists();
        renderLists.region = region;

        GraphNodeStorage nodes = region.graphData;

        int baseChunkX = region.getChunkX();
        int baseChunkY = region.getChunkY();
        int baseChunkZ = region.getChunkZ();

        for (int queueIndex = 0; queueIndex < SearchQueueUnsafe.getQueueSize(pSelf); queueIndex++) {
            var sectionIndex = SearchQueueUnsafe.getQueueEntry(pSelf, queueIndex);
            var incomingDirections = SearchQueueUnsafe.getIncomingDirections(pSelf, sectionIndex);

            var x = baseChunkX + LocalSectionIndex.unpackX(sectionIndex);
            var y = baseChunkY + LocalSectionIndex.unpackY(sectionIndex);
            var z = baseChunkZ + LocalSectionIndex.unpackZ(sectionIndex);

            if (!this.isWithinRenderDistance(x, y, z) || !isVisible(this.frustum, x, y, z)) {
                continue;
            }

            var node = nodes.getNode(sectionIndex);

            if (!GraphNode.isLoaded(node)) {
                continue;
            }

            renderLists.add(GraphNode.unpackFlags(node), (byte) sectionIndex);

            var nodeConnections = this.useOcclusionCulling ? GraphNode.unpackConnections(node) : -1;

            var outgoingDirections = VisibilityEncoding.getOutgoingDirections(nodeConnections, incomingDirections);
            outgoingDirections &= this.getValidDirections(x, y, z);

            if (outgoingDirections != 0) {
                this.searchNeighborNodes(pAdjacent, sectionIndex, outgoingDirections);
            }
        }

        return renderLists;
    }

    private boolean isWithinRenderDistance(int x, int y, int z) {
        int xDist = Math.abs(x - this.midX);
        int yDist = Math.abs(y - this.midY);
        int zDist = Math.abs(z - this.midZ);

        return Math.max(xDist, Math.max(yDist, zDist)) < this.renderDistance;
    }

    // Attempts to traverse into each neighbor of the node, if the outgoing directions of that node allows it.
    private void searchNeighborNodes(long pAdjacent, int sectionIndex, int connections) {
        {
            var adjacentSectionIndex = LocalSectionIndex.decX(sectionIndex);
            SearchQueueUnsafe.enqueueConditionally(SearchQueueAccessUnsafe.west(pAdjacent, adjacentSectionIndex > sectionIndex),
                    adjacentSectionIndex, GraphDirection.EAST, GraphDirection.bit(connections, GraphDirection.WEST));
        }

        {
            var adjacentSectionIndex = LocalSectionIndex.incX(sectionIndex);
            SearchQueueUnsafe.enqueueConditionally(SearchQueueAccessUnsafe.east(pAdjacent, adjacentSectionIndex < sectionIndex),
                    adjacentSectionIndex, GraphDirection.WEST, GraphDirection.bit(connections, GraphDirection.EAST));
        }

        {
            var adjacentSectionIndex = LocalSectionIndex.decY(sectionIndex);
            SearchQueueUnsafe.enqueueConditionally(SearchQueueAccessUnsafe.down(pAdjacent, adjacentSectionIndex > sectionIndex),
                    adjacentSectionIndex, GraphDirection.UP, GraphDirection.bit(connections, GraphDirection.DOWN));
        }

        {
            var adjacentSectionIndex = LocalSectionIndex.incY(sectionIndex);
            SearchQueueUnsafe.enqueueConditionally(SearchQueueAccessUnsafe.up(pAdjacent, adjacentSectionIndex < sectionIndex),
                    adjacentSectionIndex, GraphDirection.DOWN, GraphDirection.bit(connections, GraphDirection.UP));
        }

        {
            var adjacentSectionIndex = LocalSectionIndex.decZ(sectionIndex);
            SearchQueueUnsafe.enqueueConditionally(SearchQueueAccessUnsafe.north(pAdjacent, adjacentSectionIndex > sectionIndex),
                    adjacentSectionIndex, GraphDirection.SOUTH, GraphDirection.bit(connections, GraphDirection.NORTH));
        }

        {
            var adjacentSectionIndex = LocalSectionIndex.incZ(sectionIndex);
            SearchQueueUnsafe.enqueueConditionally(SearchQueueAccessUnsafe.south(pAdjacent, adjacentSectionIndex < sectionIndex),
                    adjacentSectionIndex, GraphDirection.NORTH, GraphDirection.bit(connections, GraphDirection.SOUTH));
        }
    }

    // Returns a bit-field of the traversal directions which would move *away* from the search origin.
    private int getValidDirections(int x, int y, int z) {
        int planes = 0;

        planes |= x <= this.midX ? 1 << GraphDirection.WEST : 0;
        planes |= x >= this.midX ? 1 << GraphDirection.EAST : 0;

        planes |= y <= this.midY ? 1 << GraphDirection.DOWN : 0;
        planes |= y >= this.midY ? 1 << GraphDirection.UP : 0;

        planes |= z <= this.midZ ? 1 << GraphDirection.NORTH : 0;
        planes |= z >= this.midZ ? 1 << GraphDirection.SOUTH : 0;

        return planes;
    }

    @Deprecated
    public static class GraphSearchPool {
        private final HeapArena searchLists = new HeapArena(256 * 1024);
        private final ObjectArrayList<RegionRenderLists> renderLists = new ObjectArrayList<>();

        public void releaseSearchLists() {
            this.searchLists.reset();
        }

        public long createSearchList() {
            return SearchQueueUnsafe.allocate(this.searchLists);
        }

        public void releaseRenderList(ChunkRenderList list) {
            for (RegionRenderLists lists : list.batches) {
                this.releaseRenderList(lists);
            }
        }

        private void releaseRenderList(RegionRenderLists lists) {
            this.renderLists.push(lists);
        }

        public RegionRenderLists createRenderLists() {
            RegionRenderLists lists;

            if (!this.renderLists.isEmpty()) {
                lists = this.renderLists.pop();
            } else {
                lists = new RegionRenderLists();
            }

            lists.reset();

            return lists;
        }
    }
}
