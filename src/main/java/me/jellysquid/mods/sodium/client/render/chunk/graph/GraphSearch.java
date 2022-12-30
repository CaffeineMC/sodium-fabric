package me.jellysquid.mods.sodium.client.render.chunk.graph;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import me.jellysquid.mods.sodium.client.util.collections.BitArray;
import me.jellysquid.mods.sodium.client.util.frustum.Frustum;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;

public final class GraphSearch {
    private final Graph graph;
    private final Frustum frustum;
    private final boolean useOcclusionCulling;
    private final int centerChunkX, centerChunkY, centerChunkZ;

    GraphSearch(Graph graph, Camera camera, Frustum frustum, boolean useOcclusionCulling) {
        this.graph = graph;

        BlockPos cameraBlockPos = camera.getBlockPos();

        this.centerChunkX = cameraBlockPos.getX() >> 4;
        this.centerChunkY = cameraBlockPos.getY() >> 4;
        this.centerChunkZ = cameraBlockPos.getZ() >> 4;

        this.frustum = frustum;

        this.useOcclusionCulling = useOcclusionCulling;
    }

    public LongArrayList start() {
        this.graph.resetTransientState();

        var sorted = new LongArrayList();

        if (BitArray.get(this.graph.graphLoaded, this.graph.getIndex(this.centerChunkX, this.centerChunkY, this.centerChunkZ))) {
            sorted.add(ChunkSectionPos.asLong(this.centerChunkX, this.centerChunkY, this.centerChunkZ));
        } else {
            int y = MathHelper.clamp(this.centerChunkY, this.graph.world.getBottomSectionCoord(), this.graph.world.getTopSectionCoord() - 1);

            for (int x2 = -this.graph.renderDistance; x2 <= this.graph.renderDistance; x2++) {
                for (int z2 = -this.graph.renderDistance; z2 <= this.graph.renderDistance; z2++) {
                    int x = this.centerChunkX + x2;
                    int z = this.centerChunkZ + z2;

                    var sectionId = this.graph.getIndex(x, y, z);

                    if (!BitArray.get(this.graph.graphLoaded, sectionId) || this.frustumCheck(sectionId, x, y, z)) {
                        continue;
                    }

                    sorted.add(ChunkSectionPos.asLong(x, y, z));
                }
            }
        }

        sorted.sort((a, b) -> {
            int ax = this.centerChunkX - ChunkSectionPos.unpackX(a);
            int az = this.centerChunkZ - ChunkSectionPos.unpackZ(a);

            int bx = this.centerChunkX - ChunkSectionPos.unpackX(b);
            int bz = this.centerChunkZ - ChunkSectionPos.unpackZ(b);

            int ad = (ax * ax) + (az * az);
            int bd = (bx * bx) + (bz * bz);

            return Integer.compare(bd, ad);
        });

        var it = sorted.longIterator();

        while (it.hasNext()) {
            long pos = it.nextLong();

            this.graph.markVisited(this.graph.getIndex(
                    ChunkSectionPos.unpackX(pos),
                    ChunkSectionPos.unpackY(pos),
                    ChunkSectionPos.unpackZ(pos)));
        }

        return sorted;
    }

    public LongArrayList next(LongArrayList nodes) {
        var next = new LongArrayList();

        for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
            this.searchNeighbors(next, nodes.getLong(nodeIndex));
        }

        return next;
    }

    private void searchNeighbors(LongArrayList queue, long fromPos) {
        int fromX = ChunkSectionPos.unpackX(fromPos);
        int fromY = ChunkSectionPos.unpackY(fromPos);
        int fromZ = ChunkSectionPos.unpackZ(fromPos);

        int fromId = this.graph.getIndex(fromX, fromY, fromZ);

        for (int direction = 0; direction < 6; direction++) {
            if (this.useOcclusionCulling && this.isCulledByGraph(fromId, direction)) {
                continue;
            }

            this.enqueueNeighbor(queue, fromId, fromX, fromY, fromZ, direction);
        }
    }

    private void enqueueNeighbor(LongArrayList queue, int fromId, int fromX, int fromY, int fromZ, int direction) {
        int toX = fromX + DirectionUtil.getOffsetX(direction);
        int toY = fromY + DirectionUtil.getOffsetY(direction);
        int toZ = fromZ + DirectionUtil.getOffsetZ(direction);

        var toId = this.graph.getIndex(toX, toY, toZ);

        if (!BitArray.get(this.graph.graphLoaded, toId)) {
            return;
        }

        this.graph.markDirection(toId, direction);

        if (this.frustumCheck(toId, toX, toY, toZ)) {
            return;
        }

        if (!this.graph.isVisited(toId)) {
            this.graph.markVisited(toId);
            this.graph.updateCullingState(toId,
                    this.graph.getCullingState(fromId) | 1 << DirectionUtil.getOpposite(direction));

            queue.add(ChunkSectionPos.asLong(toX, toY, toZ));
        }
    }

    private boolean isCulledByGraph(int id, int direction) {
        if (this.graph.isCulled(id, direction)) {
            return true;
        }

        int outgoingDirections = this.graph.getDirections(id);
        int outgoingConnections = this.graph.getConnections(id, direction);

        if (outgoingDirections != 0) {
            return (outgoingConnections & outgoingDirections) == 0;
        }

        return false;
    }

    private boolean frustumCheck(int id, int chunkX, int chunkY, int chunkZ) {
        if (this.graph.isFrustumChecked(id)) {
            return this.graph.isFrustumCulled(id);
        }

        float x = (chunkX << 4);
        float y = (chunkY << 4);
        float z = (chunkZ << 4);

        var result = !this.frustum.isBoxVisible(x, y, z, x + 16.0f, y + 16.0f, z + 16.0f);

        if (result) {
            this.graph.markFrustumCulled(id);
        }

        this.graph.markFrustumChecked(id);

        return result;
    }
}
