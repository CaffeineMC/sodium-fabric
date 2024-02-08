package net.caffeinemc.mods.sodium.mixin.core.world.chunk;

import net.caffeinemc.mods.sodium.client.world.BitStorageExtension;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.world.level.chunk.Palette;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Objects;

@Mixin(SimpleBitStorage.class)
public class SimpleBitStorageMixin implements BitStorageExtension {
    @Shadow
    @Final
    private long[] data;

    @Shadow
    @Final
    private int valuesPerLong;

    @Shadow
    @Final
    private long mask;

    @Shadow
    @Final
    private int bits;

    @Shadow
    @Final
    private int size;

    @Override
    public <T> void sodium$unpack(T[] out, Palette<T> palette) {
        int idx = 0;

        for (long word : this.data) {
            long l = word;

            for (int j = 0; j < this.valuesPerLong; ++j) {
                out[idx] = Objects.requireNonNull(palette.valueFor((int) (l & this.mask)),
                        "Palette does not contain entry for value in storage");
                l >>= this.bits;

                if (++idx >= this.size) {
                    return;
                }
            }
        }
    }
}
