package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.render.backends.ChunkRenderState;

/**
 * Instantiates a {@link ChunkRenderContainer} type which contains the given typed render state data.
 * @param <T> The type of render state this render contains
 */
public interface ChunkRenderFactory<T extends ChunkRenderState> {
    /**
     * @param column The {@link ColumnRender} this chunk will belong to
     * @param x The x-position of the chunk section
     * @param y The y-position of the chunk section
     * @param z The z-position of the chunk section
     * @return A newly created {@link ChunkRenderContainer} with an uninitialized render state and empty render data
     */
    ChunkRenderContainer<T> create(ColumnRender<T> column, int x, int y, int z);
}
