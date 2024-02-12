package net.caffeinemc.mods.sodium.client.model.color;

import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.caffeinemc.mods.sodium.client.model.quad.blender.BlendedColorProvider;
import net.caffeinemc.mods.sodium.client.world.biome.BiomeColorSource;
import net.caffeinemc.mods.sodium.client.world.WorldSlice;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import java.util.Arrays;

public class DefaultColorProviders {
    public static ColorProvider<BlockState> adapt(BlockColor provider) {
        return new VanillaAdapter(provider);
    }

    public static ColorProvider<FluidState> adapt(FluidRenderHandler handler) {
        return new FabricFluidAdapter(handler);
    }

    public static class GrassColorProvider<T> extends BlendedColorProvider<T> {
        public static final ColorProvider<BlockState> BLOCKS = new GrassColorProvider<>();

        private GrassColorProvider() {

        }

        @Override
        protected int getColor(WorldSlice world, int x, int y, int z) {
            return world.getColor(BiomeColorSource.GRASS, x, y, z);
        }
    }

    public static class FoliageColorProvider<T> extends BlendedColorProvider<T> {
        public static final ColorProvider<BlockState> BLOCKS = new FoliageColorProvider<>();

        private FoliageColorProvider() {

        }

        @Override
        protected int getColor(WorldSlice world, int x, int y, int z) {
            return world.getColor(BiomeColorSource.FOLIAGE, x, y, z);
        }
    }

    public static class WaterColorProvider<T> extends BlendedColorProvider<T> {
        public static final ColorProvider<BlockState> BLOCKS = new WaterColorProvider<>();
        public static final ColorProvider<FluidState> FLUIDS = new WaterColorProvider<>();

        private WaterColorProvider() {

        }

        @Override
        protected int getColor(WorldSlice world, int x, int y, int z) {
            return world.getColor(BiomeColorSource.WATER, x, y, z);
        }
    }

    private static class VanillaAdapter implements ColorProvider<BlockState> {
        private final BlockColor provider;

        private VanillaAdapter(BlockColor provider) {
            this.provider = provider;
        }

        @Override
        public void getColors(WorldSlice view, BlockPos pos, BlockState state, ModelQuadView quad, int[] output) {
            Arrays.fill(output, ColorARGB.toABGR(this.provider.getColor(state, view, pos, quad.getColorIndex())));
        }
    }

    private static class FabricFluidAdapter implements ColorProvider<FluidState> {
        private final FluidRenderHandler handler;

        public FabricFluidAdapter(FluidRenderHandler handler) {
            this.handler = handler;
        }

        @Override
        public void getColors(WorldSlice view, BlockPos pos, FluidState state, ModelQuadView quad, int[] output) {
            Arrays.fill(output, this.handler.getFluidColor(view, pos, state));
        }
    }
}
