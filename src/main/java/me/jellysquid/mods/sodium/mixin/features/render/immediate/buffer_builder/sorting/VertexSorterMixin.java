package me.jellysquid.mods.sodium.mixin.features.render.immediate.buffer_builder.sorting;

import com.mojang.blaze3d.systems.VertexSorter;
import me.jellysquid.mods.sodium.client.util.sorting.VertexSorters;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(VertexSorter.class)
public interface VertexSorterMixin {
    /**
     * @author JellySquid
     * @reason Optimize vertex sorting
     */
    @Overwrite
    static VertexSorter byDistance(float x, float y, float z) {
        return VertexSorters.sortByDistance(new Vector3f(x, y, z));
    }
}
