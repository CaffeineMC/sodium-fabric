package me.jellysquid.mods.sodium.client.render.pipeline;

import me.jellysquid.mods.sodium.client.model.light.LightPipeline;
import me.jellysquid.mods.sodium.client.model.light.cache.ArrayLightDataCache;
import me.jellysquid.mods.sodium.client.model.light.flat.FlatLightPipeline;
import me.jellysquid.mods.sodium.client.model.light.smooth.SmoothLightPipeline;
import me.jellysquid.mods.sodium.client.model.quad.sink.ModelQuadSinkDelegate;
import me.jellysquid.mods.sodium.client.render.block.BlockRenderPipeline;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.fluid.FluidRenderPipeline;
import me.jellysquid.mods.sodium.client.util.rand.XoRoShiRoRandom;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.block.BlockModels;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

import java.util.Random;

public class ChunkRenderPipeline {
    private final ArrayLightDataCache lightDataCache;
    private final BlockRenderPipeline blockRenderer;
    private final FluidRenderPipeline fluidRenderer;

    private final BlockModels models;

    private final Random random = new XoRoShiRoRandom();

    public ChunkRenderPipeline(MinecraftClient client) {
        this.lightDataCache = new ArrayLightDataCache(WorldSlice.BLOCK_LENGTH);

        LightPipeline smoothLightPipeline = new SmoothLightPipeline(this.lightDataCache);
        LightPipeline flatLightPipeline = new FlatLightPipeline(this.lightDataCache);

        this.blockRenderer = new BlockRenderPipeline(client, smoothLightPipeline, flatLightPipeline);
        this.fluidRenderer = new FluidRenderPipeline(client, smoothLightPipeline, flatLightPipeline);

        this.models = client.getBakedModelManager().getBlockModels();
    }

    public void renderBlock(ChunkRenderData.Builder meshInfo, BlockState state, BlockPos pos, BlockRenderView world, ModelQuadSinkDelegate consumer, boolean cull) {
        this.blockRenderer.renderModel(meshInfo, world, this.models.getModel(state), state, pos, consumer, cull, this.random, state.getRenderingSeed(pos));
    }

    public void renderFluid(ChunkRenderData.Builder meshInfo, BlockPos.Mutable pos, WorldSlice world, ModelQuadSinkDelegate consumer, FluidState fluidState) {
        this.fluidRenderer.render(meshInfo, world, pos, consumer, fluidState);
    }

    public void init(BlockRenderView world, int x, int y, int z) {
        this.lightDataCache.init(world, x, y, z);
    }
}
