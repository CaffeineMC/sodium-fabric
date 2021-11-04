package me.jellysquid.mods.sodium.mixin.features.chunk_rendering;

import me.jellysquid.mods.sodium.client.world.cloned.PalettedContainerExtended;
import net.minecraft.util.collection.PackedIntegerArray;
import net.minecraft.world.chunk.Palette;
import net.minecraft.world.chunk.PalettedContainer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PalettedContainer.class)
public class MixinPalettedContainer<T> implements PalettedContainerExtended<T> {
    @Shadow
    private int paletteSize;

    @Shadow
    protected PackedIntegerArray data;

    @Shadow
    private Palette<T> palette;

    @Shadow
    @Final
    private T defaultValue;

    @Override
    public PackedIntegerArray getDataArray() {
        return this.data;
    }

    @Override
    public Palette<T> getPalette() {
        return this.palette;
    }

    @Override
    public T getDefaultValue() {
        return this.defaultValue;
    }

    @Override
    public int getPaletteSize() {
        return this.paletteSize;
    }
}
