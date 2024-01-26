package me.jellysquid.mods.sodium.client.render.chunk.compile.tasks;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildContext;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildOutput;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderCache;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionInfo;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionMeshParts;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import me.jellysquid.mods.sodium.client.util.task.CancellationToken;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import me.jellysquid.mods.sodium.client.world.cloned.ChunkRenderContext;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.chunk.ChunkOcclusionDataBuilder;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.BlockPos;

import java.util.Map;

/**
 * Rebuilds all the meshes of a chunk for each given render pass with non-occluded blocks. The result is then uploaded
 * to graphics memory on the main thread.
 *
 * This task takes a slice of the world from the thread it is created on. Since these slices require rather large
 * array allocations, they are pooled to ensure that the garbage collector doesn't become overloaded.
 */
public class ChunkBuilderMeshingTask extends ChunkBuilderTask<ChunkBuildOutput> {
    private final RenderSection render;
    private final ChunkRenderContext renderContext;

    private final int buildTime;

    public ChunkBuilderMeshingTask(RenderSection render, ChunkRenderContext renderContext, int time) {
        this.render = render;
        this.renderContext = renderContext;
        this.buildTime = time;
    }

    @Override
    public ChunkBuildOutput execute(ChunkBuildContext buildContext, CancellationToken cancellationToken) {
        BuiltSectionInfo.Builder renderData = new BuiltSectionInfo.Builder();
        ChunkOcclusionDataBuilder occluder = new ChunkOcclusionDataBuilder();

        ChunkBuildBuffers buffers = buildContext.buffers;
        buffers.init(renderData, this.render.getSectionIndex());

        BlockRenderCache cache = buildContext.cache;
        cache.init(this.renderContext);

        WorldSlice slice = cache.getWorldSlice();

        int minX = this.render.getOriginX();
        int minY = this.render.getOriginY();
        int minZ = this.render.getOriginZ();

        int maxX = minX + 16;
        int maxY = minY + 16;
        int maxZ = minZ + 16;

        // Initialise with minX/minY/minZ so initial getBlockState crash context is correct
        BlockPos.Mutable blockPos = new BlockPos.Mutable(minX, minY, minZ);
        BlockPos.Mutable modelOffset = new BlockPos.Mutable();

        BlockRenderContext context = new BlockRenderContext(slice);

        try {
            for (int y = minY; y < maxY; y++) {
                if (cancellationToken.isCancelled()) {
                    return null;
                }

                for (int z = minZ; z < maxZ; z++) {
                    for (int x = minX; x < maxX; x++) {
                        BlockState blockState = slice.getBlockState(x, y, z);

                        if (blockState.isAir() && !blockState.hasBlockEntity()) {
                            continue;
                        }

                        blockPos.set(x, y, z);
                        modelOffset.set(x & 15, y & 15, z & 15);

                        if (blockState.getRenderType() == BlockRenderType.MODEL) {
                            BakedModel model = cache.getBlockModels()
                                .getModel(blockState);

                            long seed = blockState.getRenderingSeed(blockPos);

                            context.update(blockPos, modelOffset, blockState, model, seed);
                            cache.getBlockRenderer()
                                .renderModel(context, buffers);
                        }

                        FluidState fluidState = blockState.getFluidState();

                        if (!fluidState.isEmpty()) {
                            cache.getFluidRenderer().render(slice, fluidState, blockPos, modelOffset, buffers);
                        }

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
        } catch (CrashException ex) {
            // Propagate existing crashes (add context)
            throw fillCrashInfo(ex.getReport(), slice, blockPos);
        } catch (Exception ex) {
            // Create a new crash report for other exceptions (e.g. thrown in getQuads)
            throw fillCrashInfo(CrashReport.create(ex, "Encountered exception while building chunk meshes"), slice, blockPos);
        }

        Map<TerrainRenderPass, BuiltSectionMeshParts> meshes = new Reference2ReferenceOpenHashMap<>();

        for (TerrainRenderPass pass : DefaultTerrainRenderPasses.ALL) {
            BuiltSectionMeshParts mesh = buffers.createMesh(pass);

            if (mesh != null) {
                meshes.put(pass, mesh);
                renderData.addRenderPass(pass);
            }
        }

        renderData.setOcclusionData(occluder.build());

        return new ChunkBuildOutput(this.render, renderData.build(), meshes, this.buildTime);
    }

    private CrashException fillCrashInfo(CrashReport report, WorldSlice slice, BlockPos pos) {
        CrashReportSection crashReportSection = report.addElement("Block being rendered", 1);

        BlockState state = null;
        try {
            state = slice.getBlockState(pos);
        } catch (Exception ignored) {}
        CrashReportSection.addBlockInfo(crashReportSection, slice, pos, state);

        crashReportSection.add("Chunk section", this.render);
        if (this.renderContext != null) {
            crashReportSection.add("Render context volume", this.renderContext.getVolume());
        }

        return new CrashException(report);
    }
}
