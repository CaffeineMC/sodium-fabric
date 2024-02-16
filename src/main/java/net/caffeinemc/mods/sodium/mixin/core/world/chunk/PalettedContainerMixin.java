package net.caffeinemc.mods.sodium.mixin.core.world.chunk;

import net.caffeinemc.mods.sodium.client.world.BitStorageExtension;
import net.caffeinemc.mods.sodium.client.world.PalettedContainerROExtension;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Objects;

@Mixin(PalettedContainer.class)
public abstract class PalettedContainerMixin<T> implements PalettedContainerROExtension<T> {

    @Shadow
    private volatile PalettedContainer.Data<T> data;

    @Shadow
    @Final
    private PalettedContainer.Strategy strategy;

    @Shadow
    public abstract PalettedContainer<T> copy();

    @Override
    public void sodium$unpack(T[] values) {
        var strategy = Objects.requireNonNull(this.strategy);

        if (values.length != strategy.size()) {
            throw new IllegalArgumentException("Array is wrong size");
        }

        var data = Objects.requireNonNull(this.data, "PalettedContainer must have data");

        var storage = (BitStorageExtension) data.storage();
        storage.sodium$unpack(values, data.palette());
    }

    @Override
    public void sodium$unpack(T[] values, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        var strategy = Objects.requireNonNull(this.strategy);

        if (values.length != strategy.size()) {
            throw new IllegalArgumentException("Array is wrong size");
        }

        var data = Objects.requireNonNull(this.data, "PalettedContainer must have data");

        var storage = data.storage();
        var palette = data.palette();

        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    int localBlockIndex = strategy.getIndex(x, y, z);

                    int paletteIndex = storage.get(localBlockIndex);
                    var paletteValue =  palette.valueFor(paletteIndex);

                    values[localBlockIndex] = paletteValue;
                }
            }
        }
    }

    @Override
    public PalettedContainerRO<T> sodium$copy() {
        return this.copy();
    }
}
