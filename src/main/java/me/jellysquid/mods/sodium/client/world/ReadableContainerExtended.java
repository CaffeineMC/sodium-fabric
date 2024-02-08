package me.jellysquid.mods.sodium.client.world;

import net.minecraft.world.level.chunk.PalettedContainerRO;

public interface ReadableContainerExtended<T> {
    @SuppressWarnings("unchecked")
    static <T> ReadableContainerExtended<T> of(PalettedContainerRO<T> container) {
        return (ReadableContainerExtended<T>) container;
    }

    static <T> PalettedContainerRO<T> clone(PalettedContainerRO<T> container) {
        if (container == null) {
            return null;
        }

        return of(container).sodium$copy();
    }

    void sodium$unpack(T[] values);
    void sodium$unpack(T[] values, int minX, int minY, int minZ, int maxX, int maxY, int maxZ);

    PalettedContainerRO<T> sodium$copy();
}
