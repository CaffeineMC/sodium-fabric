package me.jellysquid.mods.sodium.client.render.chunk.graph;

import me.jellysquid.mods.sodium.client.util.collections.BitArray;
import me.jellysquid.mods.sodium.client.util.frustum.Frustum;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.chunk.ChunkOcclusionData;
import net.minecraft.client.render.chunk.ChunkOcclusionDataBuilder;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.Arrays;

public class Graph {
    private static final long DEFAULT_OCCLUSION_DATA = calculateVisibilityData(new ChunkOcclusionDataBuilder().build());

    private final int maskXZ, maskY;
    private final int offsetZ, offsetX;

    public final long[] graphLoaded;
    public final long[] graphConnections;

    private final int[] graphSearchState;

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

        this.graphLoaded = BitArray.create(arraySize);
        this.graphConnections = new long[arraySize];
        this.graphSearchState = new int[arraySize];
    }

    public void updateConnections(int x, int y, int z, ChunkOcclusionData occlusionData) {
        this.graphConnections[this.getIndex(x, y, z)] = calculateVisibilityData(occlusionData);
    }

    public void loadSection(int x, int y, int z) {
        var i = this.getIndex(x, y, z);
        BitArray.set(this.graphLoaded, i);
        this.graphConnections[i] = DEFAULT_OCCLUSION_DATA;
    }

    public void removeSection(int x, int y, int z) {
        var i = this.getIndex(x, y, z);
        BitArray.unset(this.graphLoaded, i);
        this.graphConnections[i] = DEFAULT_OCCLUSION_DATA;
    }

    public boolean isSectionVisible(int x, int y, int z) {
        return this.isVisited(this.getIndex(x, y, z));
    }

    public void resetTransientState() {
        Arrays.fill(this.graphSearchState, 0);
    }

    public int getIndex(int x, int y, int z) {
        return ((x & this.maskXZ) << (this.offsetX)) | ((z & this.maskXZ) << this.offsetZ) | (y & this.maskY);
    }

    public GraphSearch createSearch(Camera camera, Frustum frustum, boolean useOcclusionCulling) {
        return new GraphSearch(this, camera, frustum, useOcclusionCulling);
    }

    private static long calculateVisibilityData(ChunkOcclusionData occlusionData) {
        long visibilityData = 0;

        for (int fromOrdinal = 0; fromOrdinal < DirectionUtil.COUNT; fromOrdinal++) {
            for (int toOrdinal = 0; toOrdinal < DirectionUtil.COUNT; toOrdinal++) {
                if (occlusionData == null || occlusionData.isVisibleThrough(DirectionUtil.getEnum(fromOrdinal), DirectionUtil.getEnum(toOrdinal))) {
                    visibilityData |= (1L << ((toOrdinal * DirectionUtil.COUNT) + DirectionUtil.getOpposite(fromOrdinal)));
                }
            }
        }

        return visibilityData;
    }

    public boolean isVisited(int id) {
        return (this.graphSearchState[id] & (1 << 31)) != 0;
    }

    public void markVisited(int id) {
        this.graphSearchState[id] |= (1 << 31);
    }

    public void markFrustumCulled(int id) {
        this.graphSearchState[id] |= (1 << 30);
    }

    public void markFrustumChecked(int id) {
        this.graphSearchState[id] |= (1 << 29);
    }

    public boolean isFrustumCulled(int id) {
        return (this.graphSearchState[id] & (1 << 30)) != 0;
    }

    public boolean isFrustumChecked(int id) {
        return (this.graphSearchState[id] & (1 << 29)) != 0;
    }

    public void updateCullingState(int id, int state) {
        this.graphSearchState[id] |= state << 8;
    }

    public int getCullingState(int id) {
        return (this.graphSearchState[id] >> 8) & 0b111111;
    }

    public void markDirection(int id, int dir) {
        this.graphSearchState[id] |= (1 << dir);
    }

    public int getDirections(int id) {
        return this.graphSearchState[id] & 0b111111;
    }

    public boolean isCulled(int id, int direction) {
        return (this.getCullingState(id) & (1 << direction)) != 0;
    }

    public int getConnections(int id, int direction) {
        return (int) (this.graphConnections[id] >>> (direction * 6)) & 0b111111;
    }

}
