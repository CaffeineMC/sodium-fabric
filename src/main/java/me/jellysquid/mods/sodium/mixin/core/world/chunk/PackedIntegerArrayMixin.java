package me.jellysquid.mods.sodium.mixin.core.world.chunk;

import me.jellysquid.mods.sodium.client.world.PaletteStorageExtended;
import net.minecraft.util.collection.PackedIntegerArray;
import net.minecraft.world.chunk.Palette;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Objects;

@Mixin(PackedIntegerArray.class)
public class PackedIntegerArrayMixin implements PaletteStorageExtended {
    @Shadow
    @Final
    private long[] data;

    @Shadow
    @Final
    private int elementsPerLong;

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
    public <T> void sodium$unpack(T[] out, Palette<T> palette) {
        int idx = 0;

        for (long word : this.data) {
            long l = word;

            for (int j = 0; j < this.elementsPerLong; ++j) {
                out[idx] = Objects.requireNonNull(palette.get((int) (l & this.maxValue)),
                        "Palette does not contain entry for value in storage");
                l >>= this.elementBits;

                if (++idx >= this.size) {
                    return;
                }
            }
        }
    }
}
