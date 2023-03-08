package me.jellysquid.mods.sodium.client.render.chunk.graph;

import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.util.frustum.Frustum;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.chunk.ChunkOcclusionData;
import net.minecraft.client.render.chunk.ChunkOcclusionDataBuilder;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class Graph {
    private static final short DEFAULT_OCCLUSION_DATA = calculateVisibilityData(new ChunkOcclusionDataBuilder().build());

    private final int maskXZ, maskY;
    private final int offsetZ, offsetX;

    private final int[] nodes;

    protected final World world;
    protected final int renderDistance;

    public Graph(World world, int renderDistance) {
        this.world = world;
        this.renderDistance = renderDistance;

        int sizeXZ = MathHelper.smallestEncompassingPowerOfTwo((renderDistance * 2) + 1);
        int sizeY = MathHelper.smallestEncompassingPowerOfTwo(world.getTopSectionCoord() - world.getBottomSectionCoord());

        this.maskXZ = sizeXZ - 1;
        this.maskY = sizeY - 1;

        this.offsetZ = Integer.numberOfTrailingZeros(sizeY);
        this.offsetX = this.offsetZ + Integer.numberOfTrailingZeros(sizeXZ);

        int arraySize = sizeXZ * sizeY * sizeXZ;

        this.nodes = new int[arraySize];
    }

    public void updateNode(int x, int y, int z, RenderSection section, ChunkRenderData data) {
        this.nodes[this.getIndex(x, y, z)] = GraphNode.pack(section.region.id(), data.getFlags(), calculateVisibilityData(data.getOcclusionData()));
    }

    public void addNode(int x, int y, int z, RenderSection section) {
        this.nodes[this.getIndex(x, y, z)] = GraphNode.pack(section.region.id(), section.getFlags(), DEFAULT_OCCLUSION_DATA);
    }

    public void removeNode(int x, int y, int z) {
        this.nodes[this.getIndex(x, y, z)] = 0;
    }

    public int getIndex(int x, int y, int z) {
        return ((x & this.maskXZ) << (this.offsetX)) | ((z & this.maskXZ) << this.offsetZ) | (y & this.maskY);
    }

    public GraphSearch createSearch(Camera camera, Frustum frustum, boolean useOcclusionCulling) {
        this.clearVisited();
        return new GraphSearch(this, camera, frustum, this.renderDistance, useOcclusionCulling);
    }

    public int size() {
        return 0; // TODO
    }

    public int getNode(int id) {
        return this.nodes[id];
    }

    private static short calculateVisibilityData(ChunkOcclusionData occlusionData) {
        int visibilityData = 0;

        for (Direction from : DirectionUtil.ALL_DIRECTIONS) {
            for (Direction to : DirectionUtil.ALL_DIRECTIONS) {
                if (from == to) {
                    continue;
                }

                if (occlusionData == null || occlusionData.isVisibleThrough(from, to)) {
                    visibilityData = VisibilityEncoding.addConnection(visibilityData, from.ordinal(), to.ordinal());
                }
            }
        }

        return (short) visibilityData;
    }

    private void clearVisited() {
        for (int i = 0; i < this.nodes.length; i++) {
            this.nodes[i] &= ~GraphNode.VISITED_BIT;
        }
    }

    public void markVisited(int index) {
        this.nodes[index] |= GraphNode.VISITED_BIT;
    }

    public boolean isVisited(int index) {
        return (this.nodes[index] & GraphNode.VISITED_BIT) != 0;
    }

}
