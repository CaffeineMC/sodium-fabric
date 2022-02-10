package me.jellysquid.mods.sodium.mixin.features.chunk_rendering;

import me.jellysquid.mods.sodium.interop.vanilla.mixin.SimpleBitStorageExtended;
import me.jellysquid.mods.sodium.world.slice.cloned.palette.ClonedPalette;
import net.minecraft.util.SimpleBitStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SimpleBitStorage.class)
public class MixinSimpleBitStorage implements SimpleBitStorageExtended {
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
    public <T> void copyUsingPalette(T[] out, ClonedPalette<T> palette) {
        int idx = 0;

        for (long word : this.data) {
            long l = word;

            for (int j = 0; j < this.valuesPerLong; ++j) {
                out[idx] = palette.get((int) (l & this.mask));
                l >>= this.bits;

                if (++idx >= this.size) {
                    return;
                }
            }
        }
    }
}
