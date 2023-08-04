package me.jellysquid.mods.sodium.mixin.core.world.chunk;

import me.jellysquid.mods.sodium.client.world.PaletteStorageExtended;
import net.minecraft.util.collection.EmptyPaletteStorage;
import net.minecraft.world.chunk.Palette;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Arrays;
import java.util.Objects;

@Mixin(EmptyPaletteStorage.class)
public class EmptyPaletteStorageMixin implements PaletteStorageExtended {
    @Shadow
    @Final
    private int size;

    @Override
    public <T> void sodium$unpack(T[] out, Palette<T> palette) {
        if (this.size != out.length) {
            throw new IllegalArgumentException("Array has mismatched size");
        }

        var defaultEntry = Objects.requireNonNull(palette.get(0), "Palette must have default entry");
        Arrays.fill(out, defaultEntry);
    }
}
