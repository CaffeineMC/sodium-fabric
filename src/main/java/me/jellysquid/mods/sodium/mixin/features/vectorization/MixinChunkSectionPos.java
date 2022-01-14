package me.jellysquid.mods.sodium.mixin.features.vectorization;

import jdk.incubator.vector.LongVector;
import jdk.incubator.vector.VectorOperators;
import net.minecraft.util.math.ChunkSectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(ChunkSectionPos.class)
public class MixinChunkSectionPos {
    /**
     * @author Vonr
     * @reason Use Vector API
     */
    @Overwrite
    public static long asLong(int x, int y, int z) {
//        long l = 0L;
//        l |= ((long)x & 4194303L) << 42;
//        l |= ((long)y & 1048575L) << 0;
//        l |= ((long)z & 4194303L) << 20;
//        return l;
        return LongVector.fromArray(
                LongVector.SPECIES_256,
                new long[]{0, (long)x, (long)y, (long)z},
                0
        ).and(
                LongVector.fromArray(
                    LongVector.SPECIES_256,
                    new long[]{-1L, 4194303L, 1048575L, 4194303L},
                    0
                )
        ).lanewise(
                VectorOperators.LSHL,
                LongVector.fromArray(
                        LongVector.SPECIES_256,
                        new long[]{0L, 42L, 0L, 20L},
                        0
                )
        ).reduceLanes(VectorOperators.OR);
    }
}
