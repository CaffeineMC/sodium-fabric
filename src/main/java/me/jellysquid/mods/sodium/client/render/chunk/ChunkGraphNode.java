package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkRender;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

class ChunkGraphNode<T extends ChunkRenderData> {
    public final ChunkRender<T> chunk;

    public Direction direction;

    public int propagationLevel;
    public int rebuildFrame;
    public byte cullingState;

    public ChunkGraphNode(ChunkRender<T> chunk) {
        this.chunk = chunk;
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
        return this.chunk.getBoundingBox();
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
        return this.chunk.isVisibleThrough(from, to);
    }

    public BlockPos getOrigin() {
        return this.chunk.getOrigin();
    }

    public boolean hasNeighbors() {
        return this.chunk.hasNeighbors();
    }

    public void setCullingState(byte i) {
        this.cullingState = i;
    }

    public void delete() {
        this.chunk.delete();
    }
}
