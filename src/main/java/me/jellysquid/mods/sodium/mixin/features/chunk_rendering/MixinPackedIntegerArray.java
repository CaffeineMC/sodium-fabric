package me.jellysquid.mods.sodium.mixin.features.chunk_rendering;

import me.jellysquid.mods.sodium.client.world.cloned.PackedIntegerArrayExtended;
import me.jellysquid.mods.sodium.client.world.cloned.palette.ClonedPalette;
import net.minecraft.util.collection.PackedIntegerArray;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PackedIntegerArray.class)
public class MixinPackedIntegerArray implements PackedIntegerArrayExtended {
    @Shadow
    @Final
    private long[] storage;

    @Shadow
    @Final
    private int field_24079;

    @Shadow
    @Final
    private long maxValue;

    @Shadow
    @Final
    private int elementBits;

    @Shadow
    @Final
    private int size;

    @Override
    public <T> void copyUsingPalette(T[] out, ClonedPalette<T> palette) {
        int idx = 0;

        for (long word : this.storage) {
            long l = word;

            for (int j = 0; j < this.field_24079; ++j) {
                out[idx] = palette.get((int) (l & this.maxValue));
                l >>= this.elementBits;

                if (++idx >= this.size) {
                    return;
                }
            }
        }
    }
}
