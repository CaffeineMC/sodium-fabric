package me.jellysquid.mods.sodium.client.model.quad;

import net.minecraft.block.BlockState;
import net.minecraft.state.State;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.jetbrains.annotations.Nullable;

public interface ModelQuadColorProvider<T> {
    int getColor(T state, @Nullable BlockRenderView world, @Nullable BlockPos pos, int tintIndex);
}
