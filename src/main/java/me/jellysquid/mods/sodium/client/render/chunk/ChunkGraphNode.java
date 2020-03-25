package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkRender;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

class ChunkGraphNode<T extends ChunkRenderData> {
    public final ChunkRender<T> render;

    public Direction direction;

    public int propagationLevel;
    public int rebuildFrame;
    public byte cullingState;

    public ChunkGraphNode(ChunkRender<T> render) {
        this.render = render;
        this.rebuildFrame = -1;
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
        return this.render.getBoundingBox();
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

    public boolean isVisibleThrough(Direction from, Direction to) {
        return this.render.isVisibleThrough(from, to);
    }

    public BlockPos getOrigin() {
        return this.render.getOrigin();
    }

    public boolean hasNeighbors() {
        return this.render.hasNeighbors();
    }

    public void setCullingState(byte i) {
        this.cullingState = i;
    }

    public void delete() {
        this.render.delete();
    }

    public boolean isEmpty() {
        return this.render.isEmpty();
    }
}
