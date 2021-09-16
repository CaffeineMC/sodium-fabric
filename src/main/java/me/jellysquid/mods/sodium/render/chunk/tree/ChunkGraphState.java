package me.jellysquid.mods.sodium.render.chunk.tree;

import me.jellysquid.mods.sodium.util.collections.BitArray;
import net.minecraft.util.math.Direction;
import org.joml.FrustumIntersection;

import java.util.Arrays;

public class ChunkGraphState {
    private BitArray frustumData;
    private BitArray visibleData;
    private byte[] cullingState;
    private int size;

    public ChunkGraphState() {
        this.allocate(16384);
    }

    public void rebuild(ChunkGraph<?> graph, FrustumIntersection frustum) {
        int size = graph.getNodeCount();

        if (this.size < size) {
            this.allocate(size + 8192);
        } else {
            this.frustumData.fill(false);
            this.visibleData.fill(false);

            Arrays.fill(this.cullingState, (byte) 0);
        }

        graph.getFrustumBits(this.frustumData, frustum);
    }

    private void allocate(int size) {
        this.frustumData = new BitArray(size);
        this.visibleData = new BitArray(size);
        this.cullingState = new byte[size];
        this.size = size;
    }

    public void setCullingState(int id, byte parent, Direction dir) {
        this.cullingState[id] = (byte) (parent | (1 << dir.ordinal()));
    }

    public boolean canCull(int id, Direction dir) {
        return (this.cullingState[id] & 1 << dir.ordinal()) != 0;
    }

    public byte getCullingState(int id) {
        return this.cullingState[id];
    }

    public void markVisible(int id) {
        this.visibleData.set(id);
    }

    public boolean canTraverse(int id) {
        return !this.visibleData.get(id) && this.frustumData.get(id);
    }

    public boolean isVisible(int id) {
        return this.visibleData.get(id) || this.frustumData.get(id);
    }
}
