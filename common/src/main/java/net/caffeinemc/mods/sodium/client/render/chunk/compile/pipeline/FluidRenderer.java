package net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.caffeinemc.mods.sodium.client.model.color.ColorProviderRegistry;
import net.caffeinemc.mods.sodium.client.model.light.LightPipelineProvider;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TranslucentGeometryCollector;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public abstract class FluidRenderer {
    @ExpectPlatform
    public static FluidRenderer create(ColorProviderRegistry colorRegistry, LightPipelineProvider lightPipelineProvider) {
        throw new AssertionError("Platform specific code meant to be called.");
    }

    public abstract void render(LevelSlice level, BlockState blockState, FluidState fluidState, BlockPos blockPos, BlockPos offset, TranslucentGeometryCollector collector, ChunkBuildBuffers buffers);

    @ExpectPlatform
    public static boolean renderFromVanilla() {
        throw new AssertionError("Platform specific code meant to be called.");
    }
}
