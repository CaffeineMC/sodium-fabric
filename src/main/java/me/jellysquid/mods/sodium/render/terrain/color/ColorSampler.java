package me.jellysquid.mods.sodium.render.terrain.color;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import org.jetbrains.annotations.Nullable;

public interface ColorSampler<T> {
    int getColor(T state, @Nullable BlockAndTintGetter world, @Nullable BlockPos pos, int tintIndex);
}
