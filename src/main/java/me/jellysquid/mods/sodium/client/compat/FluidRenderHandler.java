package me.jellysquid.mods.sodium.client.compat;

import me.jellysquid.mods.sodium.client.model.quad.ModelQuadColorProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.jetbrains.annotations.Nullable;

public interface FluidRenderHandler extends ModelQuadColorProvider<FluidState> {
    Sprite[] getFluidSprites(@Nullable BlockRenderView view, @Nullable BlockPos pos, FluidState state);
    int getFluidColor(@Nullable BlockRenderView view, @Nullable BlockPos pos, FluidState state);

    @Override
    default int getColor(FluidState state, @Nullable BlockRenderView world, @Nullable BlockPos pos, int tintIndex) {
        return getFluidColor(world, pos, state);
    }
}
