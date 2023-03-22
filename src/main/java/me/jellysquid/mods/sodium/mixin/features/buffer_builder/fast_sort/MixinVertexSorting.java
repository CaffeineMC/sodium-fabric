package me.jellysquid.mods.sodium.mixin.features.buffer_builder.fast_sort;

import com.google.common.primitives.Floats;
import it.unimi.dsi.fastutil.ints.IntArrays;
import me.jellysquid.mods.sodium.client.util.GeometrySort;
import net.minecraft.class_8251;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(class_8251.class)
public interface MixinVertexSorting {
    /**
     * @author
     * @reason
     */
    @Overwrite
    public static class_8251 method_49907(class_8251.class_8252 pVertexSorting$DistanceFunction0) {
        return pVector3fArray1 -> {
            float[] lvFloatArray2 = new float[pVector3fArray1.length];
            int[] lvIntArray3 = new int[pVector3fArray1.length];
            for (int lvInt4 = 0; lvInt4 < pVector3fArray1.length; ++lvInt4) {
                lvFloatArray2[lvInt4] = pVertexSorting$DistanceFunction0.apply(pVector3fArray1[lvInt4]);
                lvIntArray3[lvInt4] = lvInt4;
            }

            GeometrySort.mergeSort(lvIntArray3, lvFloatArray2);
            return lvIntArray3;
        };
    }
}
