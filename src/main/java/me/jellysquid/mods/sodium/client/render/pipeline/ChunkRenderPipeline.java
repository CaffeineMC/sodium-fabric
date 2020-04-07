package me.jellysquid.mods.sodium.client.render.pipeline;

import me.jellysquid.mods.sodium.client.render.chunk.ChunkSlice;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkMeshInfo;
import me.jellysquid.mods.sodium.client.render.light.cache.ChunkLightDataCache;
import me.jellysquid.mods.sodium.client.render.quad.ModelQuadTransformer;
import me.jellysquid.mods.sodium.client.util.rand.XoRoShiRoRandom;
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

    private final BlockModels models;

    private final Random random = new XoRoShiRoRandom();

    public ChunkRenderPipeline(MinecraftClient client) {
        this.lightDataCache = new ChunkLightDataCache(ChunkSlice.BLOCK_LENGTH);

        this.blockRenderer = new BlockRenderPipeline(client, this.lightDataCache);
        this.fluidRenderer = new FluidRenderPipeline();

        this.models = client.getBakedModelManager().getBlockModels();
    }

    public boolean renderBlock(ChunkMeshInfo.Builder meshInfo, BlockState state, BlockPos pos, BlockRenderView world, ModelQuadTransformer quadTransformer, BufferBuilder builder, boolean cull) {
        BlockRenderType type = state.getRenderType();

        if (type != BlockRenderType.MODEL) {
            return false;
        }

        return this.blockRenderer.renderModel(meshInfo, world, this.models.getModel(state), state, pos, quadTransformer, builder, cull, this.random, state.getRenderingSeed(pos));
    }

    public void renderFluid(ChunkMeshInfo.Builder meshInfo, BlockPos.Mutable pos, ChunkSlice region, BufferBuilder builder, FluidState fluidState) {
        this.fluidRenderer.render(meshInfo, region, pos, builder, fluidState);
    }

    public void init(BlockRenderView world, int x, int y, int z) {
        this.lightDataCache.init(world, x, y, z);
    }
}
