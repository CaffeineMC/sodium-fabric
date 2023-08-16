package me.jellysquid.mods.sodium.client.world;

import net.minecraft.world.chunk.ReadableContainer;

public interface ReadableContainerExtended<T> {
    @SuppressWarnings("unchecked")
    static <T> ReadableContainerExtended<T> of(ReadableContainer<T> container) {
        return (ReadableContainerExtended<T>) container;
    }

    static <T> ReadableContainer<T> clone(ReadableContainer<T> container) {
        if (container == null) {
            return null;
        }

        return of(container).sodium$copy();
    }

    void sodium$unpack(T[] values);
    void sodium$unpack(T[] values, int minX, int minY, int minZ, int maxX, int maxY, int maxZ);

    ReadableContainer<T> sodium$copy();
}
