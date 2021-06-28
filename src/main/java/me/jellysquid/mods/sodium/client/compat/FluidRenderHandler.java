package me.jellysquid.mods.sodium.client.compat;

import net.minecraft.client.texture.Sprite;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.jetbrains.annotations.Nullable;

public interface FluidRenderHandler {
    Sprite[] getFluidSprites(@Nullable BlockRenderView view, @Nullable BlockPos pos, FluidState state);
    int getFluidColor(@Nullable BlockRenderView view, @Nullable BlockPos pos, FluidState state);
}
