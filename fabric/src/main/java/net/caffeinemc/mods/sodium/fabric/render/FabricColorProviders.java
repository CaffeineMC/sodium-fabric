package net.caffeinemc.mods.sodium.fabric.render;

import net.caffeinemc.mods.sodium.client.model.color.ColorProvider;
import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.FluidState;

import java.util.Arrays;

public class FabricColorProviders {
    public static ColorProvider<FluidState> adapt(FluidRenderHandler handler) {
        return new FabricFluidAdapter(handler);
    }

    private static class FabricFluidAdapter implements ColorProvider<FluidState> {
        private final FluidRenderHandler handler;

        public FabricFluidAdapter(FluidRenderHandler handler) {
            this.handler = handler;
        }

        @Override
        public void getColors(LevelSlice slice, BlockPos pos, BlockPos.MutableBlockPos scratchPos, FluidState state, ModelQuadView quad, int[] output) {
            Arrays.fill(output, 0xFF000000 | this.handler.getFluidColor(slice, pos, state));
        }
    }
}
