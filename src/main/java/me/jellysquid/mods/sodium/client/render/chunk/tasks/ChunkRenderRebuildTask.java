package me.jellysquid.mods.sodium.client.render.chunk.tasks;

import me.jellysquid.mods.sodium.client.gl.compile.ChunkBuildContext;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderBounds;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.graph.VoxelBoxList;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.pipeline.context.ChunkRenderCacheLocal;
import me.jellysquid.mods.sodium.client.util.task.CancellationSource;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import me.jellysquid.mods.sodium.client.world.cloned.ChunkRenderContext;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.chunk.ChunkOcclusionDataBuilder;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

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
    private final ChunkRenderContext renderContext;
    private final int frame;

    public ChunkRenderRebuildTask(RenderSection render, ChunkRenderContext renderContext, int frame) {
        this.render = render;
        this.renderContext = renderContext;
        this.frame = frame;
    }

    @Override
    public ChunkBuildResult performBuild(ChunkBuildContext buildContext, CancellationSource cancellationSource) {
        ChunkRenderCacheLocal cache = buildContext.cache;
        cache.init(this.renderContext);

        ChunkRenderData.Builder renderData = new ChunkRenderData.Builder();
        ChunkOcclusionDataBuilder occluder = new ChunkOcclusionDataBuilder();
        ChunkRenderBounds.Builder bounds = new ChunkRenderBounds.Builder();

        ChunkBuildBuffers buffers = buildContext.buffers;
        buffers.init(renderData, this.render.getLocalId());

        WorldSlice slice = cache.getWorldSlice();

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

                        BakedModel model = cache.getBlockModels()
                                .getModel(blockState);

                        long seed = blockState.getRenderingSeed(blockPos);

                        if (cache.getBlockRenderer().renderModel(slice, blockState, blockPos, offset, model, buffers.get(layer), true, seed)) {
                            renderData.addRenderLayer(layer);
                            rendered = true;
                        }
                    }

                    FluidState fluidState = blockState.getFluidState();

                    if (!fluidState.isEmpty()) {
                        RenderLayer layer = RenderLayers.getFluidLayer(fluidState);

                        if (cache.getFluidRenderer().render(slice, fluidState, blockPos, offset, buffers.get(layer))) {
                            renderData.addRenderLayer(layer);
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

        Map<BlockRenderPass, ChunkMeshData> meshes = new EnumMap<>(BlockRenderPass.class);

        for (BlockRenderPass pass : BlockRenderPass.VALUES) {
            ChunkMeshData mesh = buffers.createMesh(pass);

            if (mesh != null) {
                meshes.put(pass, mesh);
            }
        }

        renderData.setOcclusionData(occluder.build());
        renderData.setBounds(bounds.build(this.render.getChunkPos()));
        renderData.setOcclusionBoxes(new VoxelBoxList[] {
                generateVoxelHull(slice, 8),
                generateVoxelHull(slice, 4),
                generateVoxelHull(slice, 2)
        });

        return new ChunkBuildResult(this.render, renderData.build(), meshes, this.frame);
    }

    private static VoxelBoxList generateVoxelHull(WorldSlice slice, int size) {
        boolean[][][] data = new boolean[size + 2][size + 2][size + 2];

        int width = 16 / size;

        for (int cellX = -1; cellX <= size; cellX++) {
            for (int cellY = -1; cellY <= size; cellY++) {
                for (int cellZ = -1; cellZ <= size; cellZ++) {
                    data[cellX + 1][cellY + 1][cellZ + 1] = isAreaOccluder(slice, (cellX * width), (cellY * width), (cellZ * width),
                            (cellX * width) + width, (cellY * width) + width, (cellZ * width) + width);
                }
            }
        }

        var list = new VoxelBoxList.Builder();

        for (int cellX = 0; cellX < size; cellX++) {
            for (int cellY = 0; cellY < size; cellY++) {
                for (int cellZ = 0; cellZ < size; cellZ++) {
                    if (!data[cellX + 1][cellY + 1][cellZ + 1]) {
                        continue;
                    }

                    int faces = 0b111111;

                    for (var dir : Direction.values()) {
                        var adjCellX = cellX + dir.getOffsetX();
                        var adjCellY = cellY + dir.getOffsetY();
                        var adjCellZ = cellZ + dir.getOffsetZ();

                        if (adjCellX >= -1 && adjCellY >= -1 && adjCellZ >= -1 && adjCellX <= size && adjCellY <= size && adjCellZ <= size &&
                                data[adjCellX + 1][adjCellY + 1][adjCellZ + 1]) {
                            faces &= ~(1 << dir.ordinal());
                        }
                    }

                    list.add(cellX * width, cellY * width, cellZ * width,
                            (cellX * width) + width, (cellY * width) + width, (cellZ * width) + width, faces);
                }
            }
        }

        return list.finish();
    }

    private static boolean isAreaOccluder(WorldSlice slice, int x1, int y1, int z1, int x2, int y2, int z2) {
        for (int x = x1; x < x2; x++) {
            for (int y = y1; y < y2; y++) {
                for (int z = z1; z < z2; z++) {
                    var state = slice.getBlockStateRelative(16 + x, 16 + y, 16 + z);

                    if (!state.isOpaque() && state.getBlock() != Blocks.BLUE_STAINED_GLASS) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

}
