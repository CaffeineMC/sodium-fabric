package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.backends.ChunkRenderState;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;

public class ColumnRender<T extends ChunkRenderState> {
    @SuppressWarnings("unchecked")
    private final ChunkRender<T>[] chunks = new ChunkRender[16];

    @SuppressWarnings("unchecked")
    private final ColumnRender<T>[] neighbors = new ColumnRender[6];

    private final SodiumWorldRenderer renderer;

    private World world;
    private final int chunkX, chunkZ;
    private boolean chunkPresent;

    public ColumnRender(SodiumWorldRenderer renderer, World world, int chunkX, int chunkZ, RenderFactory<T> factory) {
        this.world = world;
        this.renderer = renderer;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;

        this.neighbors[Direction.DOWN.ordinal()] = this;
        this.neighbors[Direction.UP.ordinal()] = this;

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

    public boolean isSectionEmpty(int chunkY) {
        return ChunkSection.isEmpty(this.world.getChunk(this.chunkX, this.chunkZ).getSectionArray()[chunkY]);
    }

    public interface RenderFactory<T extends ChunkRenderState> {
        ChunkRender<T> create(ColumnRender<T> column, int x, int y, int z);
    }
}
