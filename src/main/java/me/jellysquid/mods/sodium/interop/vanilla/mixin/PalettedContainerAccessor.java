package me.jellysquid.mods.sodium.interop.vanilla.mixin;

import net.minecraft.world.level.chunk.PalettedContainer;

// TODO: Replace with a real accessor
public interface PalettedContainerAccessor<T> {
    @SuppressWarnings("unchecked")
    static <T> PalettedContainer.Data<T> getData(PalettedContainer<T> container) {
        return ((PalettedContainerAccessor<T>) container).getData();
    }

    PalettedContainer.Data<T> getData();
}
