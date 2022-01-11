package me.jellysquid.mods.sodium.render.chunk.compile.tasks;

import me.jellysquid.mods.sodium.render.chunk.passes.ChunkRenderPass;
import me.jellysquid.mods.sodium.render.terrain.TerrainBuildContext;
import me.jellysquid.mods.sodium.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.render.chunk.state.ChunkMesh;
import me.jellysquid.mods.sodium.render.chunk.state.ChunkRenderBounds;
import me.jellysquid.mods.sodium.render.chunk.state.ChunkRenderData;
import me.jellysquid.mods.sodium.render.terrain.context.PreparedTerrainRenderCache;
import me.jellysquid.mods.sodium.util.tasks.CancellationSource;
import me.jellysquid.mods.sodium.world.slice.WorldSlice;
import me.jellysquid.mods.sodium.world.slice.WorldSliceData;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.chunk.ChunkOcclusionDataBuilder;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;

import java.util.Map;

/**
 * Rebuilds all the meshes of a chunk for each given render pass with non-occluded blocks. The result is then uploaded
 * to graphics memory on the main thread.
 *
 * This task takes a slice of the world from the thread it is created on. Since these slices require rather large
 * array allocations, they are pooled to ensure that the garbage collector doesn't become overloaded.
 */
public class TerrainBuildTask extends AbstractBuilderTask {
    private final RenderSection render;
    private final WorldSliceData renderContext;
    private final int frame;

    public TerrainBuildTask(RenderSection render, WorldSliceData renderContext, int frame) {
        this.render = render;
        this.renderContext = renderContext;
        this.frame = frame;
    }

    @Override
    public TerrainBuildResult performBuild(TerrainBuildContext buildContext, CancellationSource cancellationSource) {
        ChunkRenderData.Builder renderData = new ChunkRenderData.Builder();
        ChunkOcclusionDataBuilder occluder = new ChunkOcclusionDataBuilder();
        ChunkRenderBounds.Builder bounds = new ChunkRenderBounds.Builder();

        TerrainBuildBuffers buffers = buildContext.buffers;
        buffers.init(renderData);

        PreparedTerrainRenderCache renderCache = buildContext.cache;
        renderCache.init(this.renderContext);

        WorldSlice slice = renderCache.getWorldSlice();

        int minX = this.render.getOriginX();
        int minY = this.render.getOriginY();
        int minZ = this.render.getOriginZ();

        int maxX = minX + 16;
        int maxY = minY + 16;
        int maxZ = minZ + 16;

        BlockPos.Mutable blockPos = new BlockPos.Mutable();
        BlockPos.Mutable offset = new BlockPos.Mutable();

        for (int y = minY; y < maxY; y++) {
            if (cancellationSource.isCancelled()) {
                return null;
            }

            for (int z = minZ; z < maxZ; z++) {
                for (int x = minX; x < maxX; x++) {
                    BlockState blockState = slice.getBlockState(x, y, z);

                    if (blockState.isAir()) {
                        continue;
                    }

                    blockPos.set(x, y, z);
                    offset.set(x & 15, y & 15, z & 15);

                    boolean rendered = false;

                    if (blockState.getRenderType() == BlockRenderType.MODEL) {
                        RenderLayer layer = RenderLayers.getBlockLayer(blockState);

                        BakedModel model = renderCache.getBlockModels()
                                .getModel(blockState);

                        long seed = blockState.getRenderingSeed(blockPos);

                        if (renderCache.getBlockRenderer().renderModel(slice, blockState, blockPos, offset, model, buffers.get(layer), true, seed)) {
                            rendered = true;
                        }
                    }

                    FluidState fluidState = blockState.getFluidState();

                    if (!fluidState.isEmpty()) {
                        RenderLayer layer = RenderLayers.getFluidLayer(fluidState);

                        if (renderCache.getFluidRenderer().render(slice, fluidState, blockPos, offset, buffers.get(layer))) {
                            rendered = true;
                        }
                    }

                    if (blockState.hasBlockEntity()) {
                        BlockEntity entity = slice.getBlockEntity(blockPos);

                        if (entity != null) {
                            BlockEntityRenderer<BlockEntity> renderer = MinecraftClient.getInstance().getBlockEntityRenderDispatcher().get(entity);

                            if (renderer != null) {
                                renderData.addBlockEntity(entity, !renderer.rendersOutsideBoundingBox(entity));
                                rendered = true;
                            }
                        }
                    }

                    if (blockState.isOpaqueFullCube(slice, blockPos)) {
                        occluder.markClosed(blockPos);
                    }

                    if (rendered) {
                        bounds.addBlock(x & 15, y & 15, z & 15);
                    }
                }
            }
        }

        Map<ChunkRenderPass, ChunkMesh> meshes = buffers.createMeshes();

        for (ChunkRenderPass renderPass : meshes.keySet()) {
            renderData.addNonEmptyMesh(renderPass);
        }

        renderData.setOcclusionData(occluder.build());
        renderData.setBounds(bounds.build(this.render.getChunkPos()));

        return new TerrainBuildResult(this.render, renderData.build(), meshes, this.frame);
    }
}
