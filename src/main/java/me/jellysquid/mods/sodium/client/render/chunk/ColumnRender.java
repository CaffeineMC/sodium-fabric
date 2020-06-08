package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.backends.ChunkGraphicsState;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;

public class ColumnRender<T extends ChunkGraphicsState> {
    @SuppressWarnings("unchecked")
    private final ChunkRenderContainer<T>[] chunks = new ChunkRenderContainer[16];

    @SuppressWarnings("unchecked")
    private final ColumnRender<T>[] neighbors = new ColumnRender[6];

    private final SodiumWorldRenderer renderer;

    private final World world;
    private final int chunkX, chunkZ;

    private boolean chunkPresent;

    public ColumnRender(SodiumWorldRenderer renderer, World world, int chunkX, int chunkZ, ChunkRenderFactory<T> factory) {
        this.world = world;
        this.renderer = renderer;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;

        // Chunks always have a neighbor of themselves on the Y axis
        this.neighbors[Direction.DOWN.ordinal()] = this;
        this.neighbors[Direction.UP.ordinal()] = this;

        // Initialize the chunk render containers
        for (int y = 0; y < 16; y++) {
            this.chunks[y] = factory.create(this, this.chunkX, y, this.chunkZ);
        }
    }

    /**
     * Sets the neighbor of this chunk in the given direction.
     * @param dir The neighbor's direction from this chunk
     * @param adj The neighbor chunk
     */
    public void setNeighbor(Direction dir, ColumnRender<T> adj) {
        this.neighbors[dir.ordinal()] = adj;
    }

    public ColumnRender<T> getNeighbor(Direction dir) {
        return this.neighbors[dir.ordinal()];
    }

    /**
     * Returns the chunk render belonging to the given section
     * @param y The y-coordinate of the chunk section
     * @return A chunk render for the section, null if it has not been created yet or the section is out-of-bounds
     */
    public ChunkRenderContainer<T> getChunk(int y) {
        if (y < 0 || y >= this.chunks.length) {
            return null;
        }

        return this.chunks[y];
    }

    /**
     * Deletes all associated render datas from this chunk column.
     */
    public void delete() {
        for (ChunkRenderContainer<T> render : this.chunks) {
            if (render != null) {
                render.delete();
            }
        }
    }

    /**
     * Updates whether or not this render has chunk data available.
     * @param present True if the chunk this render points to is present in the world
     */
    public void setChunkPresent(boolean present) {
        this.chunkPresent = present;
    }

    /**
     * @return True if the chunk data exists in the world, otherwise false
     */
    public boolean isChunkPresent() {
        return this.chunkPresent;
    }

    /**
     * @return The encoded chunk position as a long
     */
    public long getChunkPosLong() {
        return ChunkPos.toLong(this.chunkX, this.chunkZ);
    }

    /**
     * @return The x-position of this chunk in the world
     */
    public int getChunkX() {
        return this.chunkX;
    }

    /**
     * @return The z-position of this chunk in the world
     */
    public int getChunkZ() {
        return this.chunkZ;
    }

    /**
     * @return True if this render has all its neighbor chunks loaded in the world
     */
    public boolean hasNeighborChunkData() {
        for (Direction dir : DirectionUtil.HORIZONTAL_DIRECTIONS) {
            ColumnRender<T> neighbor = this.neighbors[dir.ordinal()];

            if (neighbor == null) {
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
            if (neighbor.getNeighbor(corner) == null) {
                return false;
            }
        }

        return true;
    }

    /**
     * Called when a chunk render belonging to this column is updated. The previous render data and the updated data
     * are provided so that implementations can modify global state appropriately. This method is called from the
     * main render thread after a chunk's render data has been updated.
     *
     * @param before The render data before the chunk was updated
     * @param after The render data after the chunk was updated
     */
    public void onChunkRenderUpdated(ChunkRenderData before, ChunkRenderData after) {
        this.renderer.onChunkRenderUpdated(before, after);
    }

    /**
     * @param sectionY The y-position of the chunk section to query
     * @return True if the chunk section is non-empty in the chunk
     */
    public boolean isSectionEmpty(int sectionY) {
        return ChunkSection.isEmpty(this.world.getChunk(this.chunkX, this.chunkZ).getSectionArray()[sectionY]);
    }

}
