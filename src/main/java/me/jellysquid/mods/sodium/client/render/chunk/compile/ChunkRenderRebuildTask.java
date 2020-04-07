package me.jellysquid.mods.sodium.client.render.chunk.compile;

import me.jellysquid.mods.sodium.client.render.chunk.ChunkSlice;
import me.jellysquid.mods.sodium.client.render.mesh.ChunkMeshBuilder;
import me.jellysquid.mods.sodium.client.render.pipeline.ChunkRenderPipeline;
import me.jellysquid.mods.sodium.client.render.quad.transformers.TranslateTransformer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.chunk.ChunkOcclusionDataBuilder;
import net.minecraft.client.util.math.Vector3d;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import org.lwjgl.opengl.GL11;

public class ChunkRenderRebuildTask extends ChunkRenderBuildTask {
    private final ChunkRender<?> render;
    private final Vector3d camera;
    private final ChunkSlice slice;

    public ChunkRenderRebuildTask(ChunkBuilder builder, ChunkRender<?> render, ChunkSlice slice) {
        this.render = render;
        this.camera = builder.getCameraPosition();
        this.slice = slice;
    }

    @Override
    public ChunkRenderUploadTask performBuild(ChunkRenderPipeline pipeline, VertexBufferCache buffers) {
        pipeline.init(this.slice, this.slice.getBlockOffsetX(), this.slice.getBlockOffsetY(), this.slice.getBlockOffsetZ());

        ChunkMeshInfo.Builder meshInfo = new ChunkMeshInfo.Builder();
        ChunkOcclusionDataBuilder occluder = new ChunkOcclusionDataBuilder();

        BlockPos from = this.render.getOrigin();
        BlockPos to = from.add(16, 16, 16);

        TranslateTransformer transformer = new TranslateTransformer();

        int minX = from.getX();
        int minY = from.getY();
        int minZ = from.getZ();

        int maxX = to.getX();
        int maxY = to.getY();
        int maxZ = to.getZ();

        BlockPos.Mutable pos = new BlockPos.Mutable();

        for (int y = minY; y < maxY; y++) {
            for (int z = minZ; z < maxZ; z++) {
                for (int x = minX; x < maxX; x++) {
                    BlockState blockState = this.slice.getBlockState(x, y, z);
                    Block block = blockState.getBlock();

                    if (blockState.isAir()) {
                        continue;
                    }

                    pos.set(x, y, z);

                    if (block.getRenderType(blockState) != BlockRenderType.INVISIBLE) {
                        RenderLayer layer = RenderLayers.getBlockLayer(blockState);

                        BufferBuilder builder = buffers.get(layer);

                        if (!builder.isBuilding()) {
                            builder.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL);
                        }

                        transformer.setOffset(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);

                        pipeline.renderBlock(meshInfo, blockState, pos, this.slice, transformer, builder, true);
                    }

                    FluidState fluidState = block.getFluidState(blockState);

                    if (!fluidState.isEmpty()) {
                        RenderLayer layer = RenderLayers.getFluidLayer(fluidState);

                        BufferBuilder builder = buffers.get(layer);

                        if (!builder.isBuilding()) {
                            builder.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL);
                        }

                        pipeline.renderFluid(meshInfo, pos, this.slice, builder, fluidState);
                    }

                    if (block.hasBlockEntity()) {
                        BlockEntity entity = this.slice.getBlockEntity(pos, WorldChunk.CreationType.CHECK);

                        if (entity != null) {
                            BlockEntityRenderer<BlockEntity> renderer = BlockEntityRenderDispatcher.INSTANCE.get(entity);

                            if (renderer != null) {
                                meshInfo.addBlockEntity(entity, !renderer.rendersOutsideBoundingBox(entity));
                            }
                        }
                    }

                    if (blockState.isFullOpaque(this.slice, pos)) {
                        occluder.markClosed(pos);
                    }
                }
            }
        }

        for (RenderLayer layer : RenderLayer.getBlockLayers()) {
            BufferBuilder builder = buffers.get(layer);

            if (builder.isBuilding() && !((ChunkMeshBuilder) builder).isEmpty()) {
                if (layer == RenderLayer.getTranslucent()) {
                    builder.sortQuads((float) this.camera.x - (float) from.getX(),
                            (float) this.camera.y - (float) from.getY(),
                            (float) this.camera.z - (float) from.getZ());
                }

                builder.end();

                meshInfo.addMeshData(layer, builder.popData());
            }
        }

        meshInfo.setOcclusionData(occluder.build());

        return new Result(this.render, meshInfo.build(), this.slice);
    }

    public static class Result extends ChunkRenderUploadTask {
        private final ChunkRender<?> chunkRender;
        private final ChunkMeshInfo meshInfo;
        private final ChunkSlice slice;

        public Result(ChunkRender<?> chunkRender, ChunkMeshInfo meshInfo, ChunkSlice slice) {
            this.chunkRender = chunkRender;
            this.meshInfo = meshInfo;
            this.slice = slice;
        }

        @Override
        public void performUpload() {
            this.chunkRender.upload(this.meshInfo);
            this.chunkRender.finishRebuild(this.slice);
        }
    }
}
