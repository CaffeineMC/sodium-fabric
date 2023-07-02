package me.jellysquid.mods.sodium.client.model.color;

import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.model.quad.blender.BlendedColorProvider;
import me.jellysquid.mods.sodium.client.world.biome.BiomeColorSource;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.minecraft.block.BlockState;
import net.minecraft.client.color.block.BlockColorProvider;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;

public class DefaultColorProviders {
    public static ColorProvider<BlockState> adapt(BlockColorProvider provider) {
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
        private final BlockColorProvider provider;

        private VanillaAdapter(BlockColorProvider provider) {
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
