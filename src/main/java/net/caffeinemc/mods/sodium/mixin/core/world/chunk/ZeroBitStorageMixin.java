package net.caffeinemc.mods.sodium.mixin.core.world.chunk;

import net.caffeinemc.mods.sodium.client.world.BitStorageExtension;
import net.minecraft.util.ZeroBitStorage;
import net.minecraft.world.level.chunk.Palette;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Arrays;
import java.util.Objects;

@Mixin(ZeroBitStorage.class)
public class ZeroBitStorageMixin implements BitStorageExtension {
    @Shadow
    @Final
    private int size;

    @Override
    public <T> void sodium$unpack(T[] out, Palette<T> palette) {
        if (this.size != out.length) {
            throw new IllegalArgumentException("Array has mismatched size");
        }

        var defaultValue = Objects.requireNonNull(palette.valueFor(0), "Palette must have default entry");
        Arrays.fill(out, defaultValue);
    }
}
