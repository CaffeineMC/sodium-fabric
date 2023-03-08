package me.jellysquid.mods.sodium.client.render.chunk.graph;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import me.jellysquid.mods.sodium.client.render.chunk.GraphDirection;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderListBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.client.util.frustum.Frustum;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;

public final class GraphSearch {
    private final Graph graph;
    private final Frustum frustum;

    private final boolean useOcclusionCulling;

    private final int midX, midY, midZ; // the center of the graph search
    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;

    private final int[] state;

    GraphSearch(Graph graph, Camera camera, Frustum frustum, int renderDistance, boolean useOcclusionCulling) {
        this.graph = graph;

        BlockPos cameraBlockPos = camera.getBlockPos();

        this.midX = cameraBlockPos.getX() >> 4;
        this.midY = cameraBlockPos.getY() >> 4;
        this.midZ = cameraBlockPos.getZ() >> 4;

        this.frustum = frustum;

        this.useOcclusionCulling = useOcclusionCulling;

        // TODO: the search can wrap around into other loaded chunks if the server hasn't updated the player's position yet
        this.minX = this.midX - renderDistance;
        this.maxX = this.midX + renderDistance;

        this.minZ = this.midZ - renderDistance;
        this.maxZ = this.midZ + renderDistance;

        this.minY = graph.world.getBottomSectionCoord();
        this.maxY = graph.world.getTopSectionCoord();

        this.state = new int[graph.getSize()]; // TODO: hot allocation
    }

    private void initSearch(LongArrayFIFOQueue queue) {
        var originNode = this.graph.getIndex(this.midX, this.midY, this.midZ);

        if (GraphNode.isLoaded(originNode)) {
            this.addToQueue(queue, this.midX, this.midY, this.midZ);
            this.state[originNode] = 0b111111;
        } else {
            this.initQueueFallback(queue);
        }
    }

    // not optimized. this only gets executed if the camera is not within the graph.
    private void initQueueFallback(LongArrayFIFOQueue queue) {
        int y = MathHelper.clamp(this.midY, this.graph.world.getBottomSectionCoord(), this.graph.world.getTopSectionCoord() - 1);

        var sorted = new LongArrayList();

        for (int relX = -this.graph.renderDistance; relX <= this.graph.renderDistance; relX++) {
            for (int relZ = -this.graph.renderDistance; relZ <= this.graph.renderDistance; relZ++) {
                int x = this.midX + relX;
                int z = this.midZ + relZ;

                var nodeId = this.graph.getIndex(x, y, z);
                var node = this.graph.getNode(nodeId);

                if (!GraphNode.isLoaded(node) || !isVisible(this.frustum, x, y, z)) {
                    continue;
                }

                sorted.add(ChunkSectionPos.asLong(x, y, z));
            }
        }

        sorted.sort((a, b) -> {
            int ax = this.midX - ChunkSectionPos.unpackX(a);
            int az = this.midZ - ChunkSectionPos.unpackZ(a);

            int bx = this.midX - ChunkSectionPos.unpackX(b);
            int bz = this.midZ - ChunkSectionPos.unpackZ(b);

            int ad = (ax * ax) + (az * az);
            int bd = (bx * bx) + (bz * bz);

            return Integer.compare(bd, ad);
        });


        var it = sorted.longIterator();

        while (it.hasNext()) {
            queue.enqueue(it.nextLong());
        }
    }

    public void getVisible(ChunkRenderListBuilder renderList) {
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();

        this.initSearch(queue);

        while (!queue.isEmpty()) {
            var pos = queue.dequeueLong();

            var x = ChunkSectionPos.unpackX(pos);
            var y = ChunkSectionPos.unpackY(pos);
            var z = ChunkSectionPos.unpackZ(pos);

            if (!isVisible(this.frustum, x, y, z)) {
                continue;
            }

            var id = this.graph.getIndex(x, y, z);
            var node = this.graph.getNode(id);

            if (!GraphNode.isLoaded(node)) {
                continue;
            }

            var visibilityData = this.useOcclusionCulling ? GraphNode.unpackConnections(node) : -1;
            var visibilityState = this.state[id];

            for (var incomingDirection = 0; incomingDirection < GraphDirection.COUNT; incomingDirection++) {
                if ((visibilityState & (1 << incomingDirection)) == 0) {
                    continue;
                }

                if (y > this.minY) {
                    if (canTraverse(visibilityData, incomingDirection, GraphDirection.DOWN)) {
                        this.tryEnqueueNeighbor(queue, GraphDirection.DOWN, x, y - 1, z);
                    }
                }

                if (y < this.maxY) {
                    if (canTraverse(visibilityData, incomingDirection, GraphDirection.UP)) {
                        this.tryEnqueueNeighbor(queue, GraphDirection.UP, x, y + 1, z);
                    }
                }

                if (z > this.minZ) {
                    if (canTraverse(visibilityData, incomingDirection, GraphDirection.NORTH)) {
                        this.tryEnqueueNeighbor(queue, GraphDirection.NORTH, x, y, z - 1);
                    }
                }

                if (z < this.maxZ) {
                    if (canTraverse(visibilityData, incomingDirection, GraphDirection.SOUTH)) {
                        this.tryEnqueueNeighbor(queue, GraphDirection.SOUTH, x, y, z + 1);
                    }
                }

                if (x > this.minX) {
                    if (canTraverse(visibilityData, incomingDirection, GraphDirection.EAST)) {
                        this.tryEnqueueNeighbor(queue, GraphDirection.EAST, x - 1, y, z);
                    }
                }

                if (x < this.maxX) {
                    if (canTraverse(visibilityData, incomingDirection, GraphDirection.WEST)) {
                        this.tryEnqueueNeighbor(queue, GraphDirection.WEST, x + 1, y, z);
                    }
                }
            }

            renderList.add(GraphNode.unpackFlags(node), GraphNode.unpackRegion(node), getLocalChunkIndex(x, y, z));
        }
    }

    private static boolean canTraverse(int visibilityData, int incomingDirection, int outgoingDirection) {
        return incomingDirection != outgoingDirection && isVisibleThrough(visibilityData, incomingDirection, outgoingDirection);
    }

    private static boolean isVisibleThrough(int visibilityData, int incomingDirection, int outgoingDirection) {
        return VisibilityEncoding.isConnected(visibilityData, incomingDirection, outgoingDirection);
    }

    private void tryEnqueueNeighbor(LongArrayFIFOQueue queue, int direction, int x, int y, int z) {
        var index = this.graph.getIndex(x, y, z);
        var state = this.state[index];

        if (state == 0) { // first time being explored
            this.addToQueue(queue, x, y, z);
        }

        // mark this direction
        this.state[index] = state | (1 << GraphDirection.opposite(direction));
    }

    private void addToQueue(LongArrayFIFOQueue queue, int x, int y, int z) {
        queue.enqueue(ChunkSectionPos.asLong(x, y, z));
    }

    private static boolean isVisible(Frustum frustum, int chunkX, int chunkY, int chunkZ) {
        float posX = (chunkX << 4);
        float posY = (chunkY << 4);
        float posZ = (chunkZ << 4);

        return frustum.isBoxVisible(posX, posY, posZ, posX + 16.0f, posY + 16.0f, posZ + 16.0f);
    }

    private static int getLocalChunkIndex(int x, int y, int z) {
        return RenderRegion.getChunkIndex(x & 7, y & 3, z & 7); // TODO: eliminate these constants
    }
}
