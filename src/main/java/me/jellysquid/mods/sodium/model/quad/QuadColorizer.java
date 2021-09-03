package me.jellysquid.mods.sodium.model.quad;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.jetbrains.annotations.Nullable;

public interface QuadColorizer<T> {
    int getColor(T state, @Nullable BlockRenderView world, @Nullable BlockPos pos, int tintIndex);
}
