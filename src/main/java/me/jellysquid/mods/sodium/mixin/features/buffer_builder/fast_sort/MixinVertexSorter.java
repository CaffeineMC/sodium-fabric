package me.jellysquid.mods.sodium.mixin.features.buffer_builder.fast_sort;

import com.mojang.blaze3d.systems.VertexSorter;
import me.jellysquid.mods.sodium.client.util.GeometrySort;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(VertexSorter.class)
public interface MixinVertexSorter {
    /**
     * @author IMS
     * @reason Optimize vertex sorting
     */
    @Overwrite
    public static VertexSorter of(VertexSorter.SortKeyMapper sortKeyMapper) {
        return pVector3fArray1 -> {
            float[] lvFloatArray2 = new float[pVector3fArray1.length];
            int[] lvIntArray3 = new int[pVector3fArray1.length];
            for (int lvInt4 = 0; lvInt4 < pVector3fArray1.length; ++lvInt4) {
                lvFloatArray2[lvInt4] = sortKeyMapper.apply(pVector3fArray1[lvInt4]);
                lvIntArray3[lvInt4] = lvInt4;
            }

            GeometrySort.mergeSort(lvIntArray3, lvFloatArray2);
            return lvIntArray3;
        };
    }
}
