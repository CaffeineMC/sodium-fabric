package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.minecraft.util.math.Direction;

public class ChunkRenderColumn<T extends ChunkGraphicsState> {
    @SuppressWarnings("unchecked")
    private final ChunkRenderContainer<T>[] renders = new ChunkRenderContainer[16];

    @SuppressWarnings("unchecked")
    private final ChunkRenderColumn<T>[] adjacent = new ChunkRenderColumn[6];

    private final int x, z;

    public ChunkRenderColumn(int x, int z) {
        this.x = x;
        this.z = z;

        this.setAdjacentColumn(Direction.UP, this);
        this.setAdjacentColumn(Direction.DOWN, this);
    }

    public void setAdjacentColumn(Direction dir, ChunkRenderColumn<T> column) {
        this.adjacent[dir.ordinal()] = column;
    }

    public ChunkRenderColumn<T> getAdjacentColumn(Direction dir) {
        return this.adjacent[dir.ordinal()];
    }

    public void setRender(int y, ChunkRenderContainer<T> render) {
        this.renders[y] = render;
    }

    public ChunkRenderContainer<T> getRender(int y) {
        if (y < 0 || y >= this.renders.length) {
            return null;
        }
        return this.renders[y];
    }

    public int getX() {
        return this.x;
    }

    public int getZ() {
        return this.z;
    }

    public boolean areNeighborsPresent() {
        for (Direction dir : DirectionUtil.HORIZONTAL_DIRECTIONS) {
            ChunkRenderColumn<T> adj = this.adjacent[dir.ordinal()];

            if (adj == null) {
                return false;
            }

            Direction corner;

            // Access the adjacent corner chunk from the neighbor in this direction
            if (dir == Direction.NORTH) {
                corner = Direction.EAST;
            } else if (dir == Direction.SOUTH) {
                corner = Direction.WEST;
            } else if (dir == Direction.WEST) {
                corner = Direction.NORTH;
            } else if (dir == Direction.EAST) {
                corner = Direction.SOUTH;
            } else {
                continue;
            }

            // If no neighbor has been attached, the chunk is not present
            if (adj.getAdjacentColumn(corner) == null) {
                return false;
            }
        }

        return true;
    }
}
