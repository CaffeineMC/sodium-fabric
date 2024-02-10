package net.caffeinemc.mods.sodium.client.world;

import net.minecraft.world.level.chunk.PalettedContainerRO;

/**
 * Provides optimized functions for cloning and unpacking {@link PalettedContainerRO}, which are faster than calling
 * {@link PalettedContainerRO#get(int, int, int)} for each value.
 *
 * @param <T> The type of entries stored in the container
 */
public interface PalettedContainerROExtension<T> {
    @SuppressWarnings("unchecked")
    static <T> PalettedContainerROExtension<T> of(PalettedContainerRO<T> container) {
        return (PalettedContainerROExtension<T>) container;
    }


    /**
     * @see PalettedContainerROExtension<T>#clone()
     * @param container The container to clone
     * @return The cloned container, or null if the input itself was null
     */
    static <T> PalettedContainerRO<T> clone(PalettedContainerRO<T> container) {
        if (container == null) {
            return null;
        }

        return of(container).sodium$copy();
    }

    /**
     * Unpacks the values into a flat array.
     *
     * @param values The array to store the values within, using the same indexing strategy as the container
     * @throws IllegalArgumentException If the array is not large enough to hold all values
     * @throws NullPointerException If the container has no data to unpack
     */
    void sodium$unpack(T[] values);

    /**
     * Unpacks the values within the bounding box into a flat array. The block coordinates provided
     * are relative to the container, and should be in the range of [0, 15].
     *
     * @param values The array to store the values within, using the same indexing strategy as the container
     * @param minX The minimum x-coordinate of the bounding box (inclusive)
     * @param minY The minimum y-coordinate of the bounding box (inclusive)
     * @param minZ The minimum z-coordinate of the bounding box (inclusive)
     * @param maxX The maximum x-coordinate of the bounding box (inclusive)
     * @param maxY The maximum y-coordinate of the bounding box (inclusive)
     * @param maxZ The maximum z-coordinate of the bounding box (inclusive)
     * @throws IllegalArgumentException If the array is not large enough to hold all values
     * @throws NullPointerException If the container has no data to unpack
     */
    void sodium$unpack(T[] values, int minX, int minY, int minZ, int maxX, int maxY, int maxZ);

    /**
     * Clones the {@param container} and any contents. This can be used to make thread-safe copies of world state.
     *
     * @return A cloned instance of the container, which is memory-independent of the original
    */
    PalettedContainerRO<T> sodium$copy();
}
