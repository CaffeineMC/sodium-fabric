package me.jellysquid.mods.sodium.client.world.cloned;

import me.jellysquid.mods.sodium.client.world.cloned.palette.ClonedPalette;
import net.minecraft.util.collection.PackedIntegerArray;
import net.minecraft.world.chunk.Palette;
import net.minecraft.world.chunk.PalettedContainer;

public interface PalettedContainerExtended<T> {
    @SuppressWarnings("unchecked")
    static <T> PalettedContainerExtended<T> cast(PalettedContainer<T> container) {
        return (PalettedContainerExtended<T>) container;
    }

    PackedIntegerArray getDataArray();

    Palette<T> getPalette();

    T getDefaultValue();

    int getPaletteSize();
}
