package net.caffeinemc.mods.sodium.neoforge.render;

import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.client.model.color.ColorProvider;
import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

import java.util.Arrays;

public class ForgeColorProviders {
    public static ColorProvider<FluidState> adapt(IClientFluidTypeExtensions handler) {
        return new ForgeFluidAdapter(handler);
    }

    private static class ForgeFluidAdapter implements ColorProvider<FluidState> {
        private final IClientFluidTypeExtensions handler;

        public ForgeFluidAdapter(IClientFluidTypeExtensions handler) {
            this.handler = handler;
        }

        @Override
        public void getColors(LevelSlice slice, BlockPos pos, BlockPos.MutableBlockPos scratchPos, FluidState state, ModelQuadView quad, int[] output) {
            Arrays.fill(output,this.handler.getTintColor(state, slice, pos));
        }
    }
}
