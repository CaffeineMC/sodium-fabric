package me.jellysquid.mods.sodium.mixin.core.world.chunk;

import me.jellysquid.mods.sodium.client.world.PaletteStorageExtended;
import me.jellysquid.mods.sodium.client.world.ReadableContainerExtended;
import net.minecraft.world.chunk.PalettedContainer;
import net.minecraft.world.chunk.ReadableContainer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Objects;

@Mixin(PalettedContainer.class)
public abstract class PalettedContainerMixin<T> implements ReadableContainerExtended<T> {

    @Shadow
    private volatile PalettedContainer.Data<T> data;

    @Shadow
    @Final
    private PalettedContainer.PaletteProvider paletteProvider;

    @Shadow
    public abstract PalettedContainer<T> copy();

    @Override
    public void sodium$unpack(T[] values) {
        var indexer = Objects.requireNonNull(this.paletteProvider);

        if (values.length != indexer.getContainerSize()) {
            throw new IllegalArgumentException("Array is wrong size");
        }

        var data = Objects.requireNonNull(this.data, "PalettedContainer must have data");

        var storage = (PaletteStorageExtended) data.storage();
        storage.sodium$unpack(values, data.palette());
    }

    @Override
    public void sodium$unpack(T[] values, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        var indexer = Objects.requireNonNull(this.paletteProvider);

        if (values.length != indexer.getContainerSize()) {
            throw new IllegalArgumentException("Array is wrong size");
        }

        var data = Objects.requireNonNull(this.data, "PalettedContainer must have data");

        var storage = data.storage();
        var palette = data.palette();

        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    int localBlockIndex = indexer.computeIndex(x, y, z);

                    int paletteIndex = storage.get(localBlockIndex);
                    var paletteValue =  palette.get(paletteIndex);

                    values[localBlockIndex] = paletteValue;
                }
            }
        }
    }

    @Override
    public ReadableContainer<T> sodium$copy() {
        return this.copy();
    }
}
