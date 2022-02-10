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
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
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
        VisGraph occluder = new VisGraph();
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

        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos offset = new BlockPos.MutableBlockPos();

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

                    if (blockState.getRenderShape() == RenderShape.MODEL) {
                        RenderType layer = ItemBlockRenderTypes.getChunkRenderType(blockState);

                        BakedModel model = renderCache.getBlockModels()
                                .getBlockModel(blockState);

                        long seed = blockState.getSeed(blockPos);

                        if (renderCache.getBlockRenderer().renderModel(slice, blockState, blockPos, offset, model, buffers.get(layer), true, seed)) {
                            rendered = true;
                        }
                    }

                    FluidState fluidState = blockState.getFluidState();

                    if (!fluidState.isEmpty()) {
                        RenderType layer = ItemBlockRenderTypes.getRenderLayer(fluidState);

                        if (renderCache.getFluidRenderer().render(slice, fluidState, blockPos, offset, buffers.get(layer))) {
                            rendered = true;
                        }
                    }

                    if (blockState.hasBlockEntity()) {
                        BlockEntity entity = slice.getBlockEntity(blockPos);

                        if (entity != null) {
                            BlockEntityRenderer<BlockEntity> renderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(entity);

                            if (renderer != null) {
                                renderData.addBlockEntity(entity, !renderer.shouldRenderOffScreen(entity));
                                rendered = true;
                            }
                        }
                    }

                    if (blockState.isSolidRender(slice, blockPos)) {
                        occluder.setOpaque(blockPos);
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

        renderData.setOcclusionData(occluder.resolve());
        renderData.setBounds(bounds.build(this.render.getChunkPos()));

        return new TerrainBuildResult(this.render, renderData.build(), meshes, this.frame);
    }
}
