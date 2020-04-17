package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.render.FrustumExtended;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.backends.ChunkRenderState;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class ColumnRender<T extends ChunkRenderState> {
    @SuppressWarnings("unchecked")
    private final ChunkRender<T>[] chunks = new ChunkRender[16];

    @SuppressWarnings("unchecked")
    private final ColumnRender<T>[] neighbors = new ColumnRender[6];

    private final SodiumWorldRenderer renderer;

    private final int chunkX, chunkZ;

    private final float boundsMinX;
    private final float boundsMinY;
    private final float boundsMinZ;

    private final float boundsMaxX;
    private final float boundsMaxY;
    private final float boundsMaxZ;

    private boolean chunkPresent;
    private boolean visible;
    private int lastFrame = -1;

    public ColumnRender(SodiumWorldRenderer renderer, World world, int chunkX, int chunkZ, RenderFactory<T> factory) {
        this.renderer = renderer;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;

        int x = chunkX << 4;
        int z = chunkZ << 4;

        this.neighbors[Direction.DOWN.ordinal()] = this;
        this.neighbors[Direction.UP.ordinal()] = this;

        this.boundsMinX = x;
        this.boundsMinY = Float.NEGATIVE_INFINITY;
        this.boundsMinZ = z;

        this.boundsMaxX = x + 16.0f;
        this.boundsMaxY = Float.POSITIVE_INFINITY;
        this.boundsMaxZ = x + 16.0f;

        for (int y = 0; y < 16; y++) {
            this.chunks[y] = factory.create(this, this.chunkX, y, this.chunkZ);
        }
    }

    public void setNeighbor(Direction dir, ColumnRender<T> adj) {
        this.neighbors[dir.ordinal()] = adj;
    }

    public ColumnRender<T> getNeighbor(Direction dir) {
        return this.neighbors[dir.ordinal()];
    }

    public ChunkRender<T> getChunk(int y) {
        if (y < 0 || y >= this.chunks.length) {
            return null;
        }

        return this.chunks[y];
    }

    public void delete() {
        for (ChunkRender<T> render : this.chunks) {
            if (render != null) {
                render.delete();
            }
        }
    }

    public void setChunkPresent(boolean flag) {
        this.chunkPresent = flag;
    }

    public boolean isChunkPresent() {
        return this.chunkPresent;
    }

    public long getKey() {
        return ChunkPos.toLong(this.chunkX, this.chunkZ);
    }

    public boolean isVisible(FrustumExtended frustum, int frame) {
        if (this.lastFrame == frame) {
            return this.visible;
        }

        this.visible = frustum.fastAabbTest(this.boundsMinX, this.boundsMinY, this.boundsMinZ, this.boundsMaxX, this.boundsMaxY, this.boundsMaxZ);
        this.lastFrame = frame;

        return this.visible;
    }

    public int getX() {
        return this.chunkX;
    }

    public int getZ() {
        return this.chunkZ;
    }

    public boolean hasNeighbors() {
        for (Direction dir : DirectionUtil.HORIZONTAL_DIRECTIONS) {
            ColumnRender<T> neighbor = this.neighbors[dir.ordinal()];

            if (neighbor == null) {
                return false;
            }

            Direction corner;

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

            if (neighbor.getNeighbor(corner) == null) {
                return false;
            }
        }

        return true;
    }

    public void onChunkRenderUpdated(ChunkRenderData before, ChunkRenderData after) {
        this.renderer.onChunkRenderUpdated(before, after);
    }

    public interface RenderFactory<T extends ChunkRenderState> {
        ChunkRender<T> create(ColumnRender<T> column, int x, int y, int z);
    }
}
