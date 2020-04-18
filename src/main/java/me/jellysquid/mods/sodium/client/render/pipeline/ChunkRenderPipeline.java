package me.jellysquid.mods.sodium.client.render.pipeline;

import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.light.cache.ChunkLightDataCache;
import me.jellysquid.mods.sodium.client.render.light.flat.FlatLightPipeline;
import me.jellysquid.mods.sodium.client.render.light.smooth.SmoothLightPipeline;
import me.jellysquid.mods.sodium.client.render.model.quad.ModelQuadTransformer;
import me.jellysquid.mods.sodium.client.util.rand.XoRoShiRoRandom;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.block.BlockModels;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

import java.util.Random;

public class ChunkRenderPipeline {
    private final ChunkLightDataCache lightDataCache;
    private final BlockRenderPipeline blockRenderer;
    private final FluidRenderPipeline fluidRenderer;

    private final SmoothLightPipeline smoothLightPipeline;
    private final FlatLightPipeline flatLightPipeline;

    private final BlockModels models;

    private final Random random = new XoRoShiRoRandom();

    public ChunkRenderPipeline(MinecraftClient client) {
        this.lightDataCache = new ChunkLightDataCache(WorldSlice.BLOCK_LENGTH);

        this.smoothLightPipeline = new SmoothLightPipeline(this.lightDataCache);
        this.flatLightPipeline = new FlatLightPipeline(this.lightDataCache);

        this.blockRenderer = new BlockRenderPipeline(client, this.smoothLightPipeline, this.flatLightPipeline);
        this.fluidRenderer = new FluidRenderPipeline(client, this.smoothLightPipeline, this.flatLightPipeline);

        this.models = client.getBakedModelManager().getBlockModels();
    }

    public boolean renderBlock(ChunkRenderData.Builder meshInfo, BlockState state, BlockPos pos, BlockRenderView world, ModelQuadTransformer quadTransformer, BufferBuilder builder, boolean cull) {
        BlockRenderType type = state.getRenderType();

        if (type != BlockRenderType.MODEL) {
            return false;
        }

        return this.blockRenderer.renderModel(meshInfo, world, this.models.getModel(state), state, pos, quadTransformer, builder, cull, this.random, state.getRenderingSeed(pos));
    }

    public void renderFluid(ChunkRenderData.Builder meshInfo, BlockPos.Mutable pos, WorldSlice region, BufferBuilder builder, FluidState fluidState) {
        this.fluidRenderer.render(meshInfo, region, pos, builder, fluidState);
    }

    public void init(BlockRenderView world, int x, int y, int z) {
        this.lightDataCache.init(world, x, y, z);
    }
}
