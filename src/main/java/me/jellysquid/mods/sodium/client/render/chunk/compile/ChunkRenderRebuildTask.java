package me.jellysquid.mods.sodium.client.render.chunk.compile;

import me.jellysquid.mods.sodium.client.render.chunk.ChunkSlice;
import me.jellysquid.mods.sodium.client.render.chunk.CloneableBufferBuilder;
import me.jellysquid.mods.sodium.client.render.mesh.ChunkMeshBuilder;
import me.jellysquid.mods.sodium.client.render.pipeline.ChunkRenderPipeline;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.chunk.ChunkOcclusionData;
import net.minecraft.client.render.chunk.ChunkOcclusionDataBuilder;
import net.minecraft.client.util.math.Vector3d;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.WorldChunk;
import org.lwjgl.opengl.GL11;

import java.util.EnumSet;

public class ChunkRenderRebuildTask extends ChunkRenderBuildTask {
    private static final ChunkOcclusionData EMPTY_OCCLUSION_DATA;

    static {
        ChunkOcclusionData data = new ChunkOcclusionData();
        data.addOpenEdgeFaces(EnumSet.allOf(Direction.class));

        EMPTY_OCCLUSION_DATA = data;
    }

    private final ChunkRender<?> render;
    private final Vector3d camera;
    private final ChunkSlice region;
    private final ChunkRenderPipeline pipeline;
    private final BlockRenderManager fallbackPipeline;

    public ChunkRenderRebuildTask(ChunkBuilder builder, ChunkRender<?> render, ChunkSlice slice) {
        this.render = render;
        this.camera = builder.getCameraPosition();
        this.region = slice;

        this.pipeline = new ChunkRenderPipeline(MinecraftClient.getInstance(), slice);
        this.fallbackPipeline = MinecraftClient.getInstance().getBlockRenderManager();
    }

    @Override
    public ChunkRenderUploadTask performBuild(VertexBufferCache buffers) {
        ChunkMeshInfo.Builder info = new ChunkMeshInfo.Builder();
        ChunkOcclusionDataBuilder occluder = new ChunkOcclusionDataBuilder();

        Vector3f translation = new Vector3f();

        BlockPos from = this.render.getOrigin();
        BlockPos to = from.add(16, 16, 16);

        int minX = from.getX();
        int minY = from.getY();
        int minZ = from.getZ();

        int maxX = to.getX();
        int maxY = to.getY();
        int maxZ = to.getZ();

        BlockPos.Mutable pos = new BlockPos.Mutable();

        for (int z = minZ; z < maxZ; z++) {
            for (int y = minY; y < maxY; y++) {
                for (int x = minX; x < maxX; x++) {
                    BlockState blockState = this.region.getBlockState(x, y, z);
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

                        translation.set(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);

                        this.pipeline.renderBlock(blockState, pos, this.region, translation, builder, true);
                    }

                    FluidState fluidState = block.getFluidState(blockState);

                    if (!fluidState.isEmpty()) {
                        RenderLayer layer = RenderLayers.getFluidLayer(fluidState);

                        BufferBuilder builder = buffers.get(layer);

                        if (!builder.isBuilding()) {
                            builder.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL);
                        }

                        this.fallbackPipeline.renderFluid(pos, this.region, builder, fluidState);
                    }

                    if (block.hasBlockEntity()) {
                        BlockEntity entity = this.region.getBlockEntity(pos, WorldChunk.CreationType.CHECK);

                        if (entity != null) {
                            BlockEntityRenderer<BlockEntity> renderer = BlockEntityRenderDispatcher.INSTANCE.get(entity);

                            if (renderer != null) {
                                info.addBlockEntity(entity, renderer.rendersOutsideBoundingBox(entity));
                            }
                        }
                    }

                    if (blockState.isFullOpaque(this.region, pos)) {
                        occluder.markClosed(pos);
                    }
                }
            }
        }

        for (RenderLayer layer : RenderLayer.getBlockLayers()) {
            BufferBuilder builder = buffers.get(layer);

            if (!builder.isBuilding() || ((ChunkMeshBuilder) builder).isEmpty()) {
                continue;
            }

            if (layer == RenderLayer.getTranslucent()) {
                builder.sortQuads((float) this.camera.x - (float) from.getX(),
                        (float) this.camera.y - (float) from.getY(),
                        (float) this.camera.z - (float) from.getZ());
            }

            // TODO: simplify API
            builder.end();

            info.addMeshData(layer, ((CloneableBufferBuilder) builder).copyData());
        }

        info.setOcclusionData(occluder.build());

        return new Result(this.render, info.build());
    }

    public static class Result extends ChunkRenderUploadTask {
        private final ChunkRender<?> chunkRender;
        private final ChunkMeshInfo meshInfo;

        public Result(ChunkRender<?> chunkRender, ChunkMeshInfo meshInfo) {
            this.chunkRender = chunkRender;
            this.meshInfo = meshInfo;
        }

        @Override
        public void performUpload() {
            if (this.chunkRender.isInvalid()) {
                return;
            }

            this.chunkRender.upload(this.meshInfo);
            this.chunkRender.finishRebuild();
        }
    }
}
