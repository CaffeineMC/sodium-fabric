package me.jellysquid.mods.sodium.client.render.chunk;

import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

class ChunkGraphNode {
    public final ChunkBuilder.BuiltChunk chunk;

    public Direction direction;
    public int propagationLevel;
    public int rebuildFrame;
    public byte cullingState;

    private final int index;

    public ChunkGraphNode(ChunkBuilder.BuiltChunk chunk, int index) {
        this.chunk = chunk;
        this.rebuildFrame = -1;
        this.index = index;
    }

    public void updateCullingState(byte parent, Direction from) {
        this.cullingState = (byte) (parent | (1 << from.ordinal()));
    }

    public boolean canCull(Direction from) {
        return (this.cullingState & 1 << from.ordinal()) > 0;
    }

    public void reset() {
        this.direction = null;
        this.propagationLevel = 0;
        this.cullingState = 0;
    }

    public void setRebuildFrame(int frame) {
        this.rebuildFrame = frame;
    }

    public Box getBoundingBox() {
        return this.chunk.boundingBox;
    }

    public void setPropagationLevel(int level) {
        this.propagationLevel = level;
    }

    public void setDirection(Direction dir) {
        this.direction = dir;
    }

    public int getRebuildFrame() {
        return this.rebuildFrame;
    }

    public int getIndex() {
        return this.index;
    }

    public boolean isVisibleThrough(Direction from, Direction to) {
        return this.chunk.getData().isVisibleThrough(from, to);
    }

    public BlockPos getOrigin() {
        return this.chunk.getOrigin();
    }
}
