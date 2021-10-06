package me.jellysquid.mods.sodium.mixin.features.chunk_rendering;

import me.jellysquid.mods.sodium.client.world.cloned.PalettedContainerExtended;
import net.minecraft.util.collection.PaletteStorage;
import net.minecraft.world.chunk.Palette;
import net.minecraft.world.chunk.PalettedContainer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PalettedContainer.class)
public abstract class MixinPalettedContainer<T> implements PalettedContainerExtended<T> {
    @Shadow
    protected PalettedContainer.Data data;

    @Shadow
    protected abstract T get(int index);

    @Shadow
    @Final
    private PalettedContainer.PaletteProvider paletteProvider;

    @Override
    public PaletteStorage getDataArray() {
        return this.data.storage();
    }

    @Override
    public Palette<T> getPalette() {
        return this.data.palette();
    }

    @Override
    public T getDefaultValue() {
        return this.get(0);
    }

    @Override
    public int getPaletteSize() {
        return this.data.configuration().bits();
    }

    @Override
    public int getPaletteContainerSize() {
        return this.paletteProvider.getContainerSize();
    }
}
