package me.jellysquid.mods.sodium.client.world.cloned;

import net.minecraft.util.collection.PaletteStorage;
import net.minecraft.world.chunk.Palette;
import net.minecraft.world.chunk.PalettedContainer;

public interface PalettedContainerExtended<T> {
    @SuppressWarnings("unchecked")
    static <T> PalettedContainerExtended<T> cast(PalettedContainer<T> container) {
        return (PalettedContainerExtended<T>) container;
    }

    PaletteStorage getDataArray();

    Palette<T> getPalette();

    T getDefaultValue();

    int getPaletteSize();

    int getContainerSize();
}
