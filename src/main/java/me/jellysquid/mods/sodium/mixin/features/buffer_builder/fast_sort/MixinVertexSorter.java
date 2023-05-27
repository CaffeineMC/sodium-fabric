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
    static VertexSorter of(VertexSorter.SortKeyMapper sortKeyMapper) {
        return locations -> {
            float[] newLocations = new float[locations.length];
            int[] indexes = new int[locations.length];
            for (int lvInt4 = 0; lvInt4 < locations.length; ++lvInt4) {
                newLocations[lvInt4] = sortKeyMapper.apply(locations[lvInt4]);
                indexes[lvInt4] = lvInt4;
            }

            GeometrySort.mergeSort(indexes, newLocations);
            return indexes;
        };
    }
}
