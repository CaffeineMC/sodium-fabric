package me.jellysquid.mods.sodium.render.chunk.tasks;

import me.jellysquid.mods.sodium.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.render.chunk.data.ChunkMeshData;
import me.jellysquid.mods.sodium.render.chunk.data.ChunkRenderBounds;
import me.jellysquid.mods.sodium.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.util.task.CancellationSource;
import me.jellysquid.mods.sodium.world.WorldSlice;
import me.jellysquid.mods.sodium.world.cloned.ChunkRenderContext;
import me.jellysquid.mods.sodium.render.renderer.TerrainRenderContext;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.chunk.ChunkOcclusionDataBuilder;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;

import java.util.EnumMap;
import java.util.Map;

/**
 * Rebuilds all the meshes of a chunk for each given render pass with non-occluded blocks. The result is then uploaded
 * to graphics memory on the main thread.
 *
 * This task takes a slice of the world from the thread it is created on. Since these slices require rather large
 * array allocations, they are pooled to ensure that the garbage collector doesn't become overloaded.
 */
public class ChunkRenderRebuildTask extends ChunkRenderBuildTask {
    private final RenderSection render;
    private final ChunkRenderContext context;
    private final int frame;
    private final int detailLevel;

    public ChunkRenderRebuildTask(RenderSection render, ChunkRenderContext context, int frame, int detailLevel) {
        this.render = render;
        this.context = context;
        this.frame = frame;
        this.detailLevel = detailLevel;
    }

    @Override
    public ChunkBuildResult performBuild(TerrainRenderContext context, ChunkBuildBuffers buffers, CancellationSource cancellationSource) {
        ChunkRenderData.Builder renderData = new ChunkRenderData.Builder();
        ChunkOcclusionDataBuilder occluder = new ChunkOcclusionDataBuilder();

        buffers.init(renderData);
        context.prepare(this.context);

        WorldSlice slice = context.getWorldSlice();

        int minX = this.render.getOriginX();
        int minY = this.render.getOriginY();
        int minZ = this.render.getOriginZ();

        int maxX = minX + 16;
        int maxY = minY + 16;
        int maxZ = minZ + 16;

        BlockPos.Mutable blockPos = new BlockPos.Mutable();

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

                    context.renderBlock(blockState, blockPos, this.detailLevel);

                    if (blockState.hasBlockEntity()) {
                        BlockEntity entity = slice.getBlockEntity(blockPos);

                        if (entity != null) {
                            BlockEntityRenderer<BlockEntity> renderer = MinecraftClient.getInstance().getBlockEntityRenderDispatcher().get(entity);

                            if (renderer != null) {
                                renderData.addBlockEntity(entity, !renderer.rendersOutsideBoundingBox(entity));
                            }
                        }
                    }

                    if (blockState.isOpaqueFullCube(slice, blockPos)) {
                        occluder.markClosed(blockPos);
                    }
                }
            }
        }

        Map<BlockRenderPass, ChunkMeshData> meshes = new EnumMap<>(BlockRenderPass.class);

        for (BlockRenderPass pass : BlockRenderPass.VALUES) {
            ChunkMeshData mesh = buffers.createMesh(pass);

            if (mesh != null) {
                meshes.put(pass, mesh);
            }
        }

        renderData.setOcclusionData(occluder.build());
        renderData.setBounds(new ChunkRenderBounds(this.render.getChunkPos()));

        return new ChunkBuildResult(this.render, renderData.build(), meshes, this.frame, this.detailLevel);
    }

    @Override
    public void releaseResources() {
        this.context.releaseResources();
    }
}
