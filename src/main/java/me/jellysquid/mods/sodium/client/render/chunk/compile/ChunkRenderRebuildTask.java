package me.jellysquid.mods.sodium.client.render.chunk.compile;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import me.jellysquid.mods.sodium.client.render.chunk.CloneableBufferBuilder;
import me.jellysquid.mods.sodium.client.render.mesh.ChunkMeshBuilder;
import me.jellysquid.mods.sodium.client.render.pipeline.ChunkRenderPipeline;
import me.jellysquid.mods.sodium.client.render.vertex.BufferUploadData;
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
import net.minecraft.client.render.chunk.ChunkRendererRegion;
import net.minecraft.client.util.math.Vector3d;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.WorldChunk;
import org.lwjgl.opengl.GL11;

import java.util.EnumSet;

public class ChunkRenderRebuildTask extends ChunkRenderBuildTask {
    private final ChunkRender<?> render;
    private final Vector3d camera;
    private final BlockPos origin;
    private final ChunkRendererRegion region;
    private final ChunkRenderPipeline pipeline;
    private final BlockRenderManager fallbackPipeline;

    public ChunkRenderRebuildTask(ChunkBuilder builder, ChunkRender<?> render) {
        this.render = render;

        this.camera = builder.getCameraPosition();
        this.origin = this.render.getOrigin();

        BlockPos from = this.origin.add(-1, -1, -1);
        BlockPos to = this.origin.add(16, 16, 16);

        this.region = ChunkRendererRegion.create(builder.getWorld(), from, to, 1);
        this.pipeline = new ChunkRenderPipeline(MinecraftClient.getInstance(), this.region, this.origin);
        this.fallbackPipeline = MinecraftClient.getInstance().getBlockRenderManager();
    }

    @Override
    public ChunkRenderUploadTask performBuild(VertexBufferCache buffers) {
        ChunkMeshInfo meshInfo;
        Object2ObjectMap<RenderLayer, BufferUploadData> uploads;

        if (this.region == null) {
            meshInfo = new ChunkMeshInfo();
            meshInfo.occlusionGraph = new ChunkOcclusionData();
            meshInfo.occlusionGraph.addOpenEdgeFaces(EnumSet.allOf(Direction.class));

            uploads = Object2ObjectMaps.emptyMap();
        } else {
            meshInfo = this.generateMesh(buffers);
            uploads = new Object2ObjectArrayMap<>();

            for (RenderLayer layer : meshInfo.presentLayers) {
                BufferBuilder builder = buffers.get(layer);
                builder.end();

                uploads.put(layer, ((CloneableBufferBuilder) builder).copyData());
            }
        }

        return new Result(this.render, meshInfo, uploads);
    }

    private ChunkMeshInfo generateMesh(VertexBufferCache buffers) {
        ChunkMeshInfo info = new ChunkMeshInfo();
        ChunkOcclusionDataBuilder occlusionDataBuilder = new ChunkOcclusionDataBuilder();

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
                    pos.set(x, y, z);

                    BlockState blockState = this.region.getBlockState(pos);
                    FluidState fluidState = blockState.getFluidState();

                    Block block = blockState.getBlock();

                    if (blockState.isFullOpaque(this.region, pos)) {
                        occlusionDataBuilder.markClosed(pos);
                    }

                    if (block.hasBlockEntity()) {
                        BlockEntity entity = this.region.getBlockEntity(pos, WorldChunk.CreationType.CHECK);

                        if (entity != null) {
                            BlockEntityRenderer<BlockEntity> renderer = BlockEntityRenderDispatcher.INSTANCE.get(entity);

                            if (renderer != null) {
                                info.blockEntities.add(entity);

                                if (renderer.rendersOutsideBoundingBox(entity)) {
                                    info.globalEntities.add(entity);
                                }
                            }
                        }
                    }

                    if (!fluidState.isEmpty()) {
                        RenderLayer layer = RenderLayers.getFluidLayer(fluidState);

                        BufferBuilder builder = buffers.get(layer);

                        if (!builder.isBuilding()) {
                            builder.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL);
                        }

                        this.fallbackPipeline.renderFluid(pos, this.region, builder, fluidState);
                    }

                    if (blockState.getRenderType() != BlockRenderType.INVISIBLE) {
                        RenderLayer layer = RenderLayers.getBlockLayer(blockState);

                        BufferBuilder builder = buffers.get(layer);

                        if (!builder.isBuilding()) {
                            builder.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL);
                        }

                        translation.set(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);

                        this.pipeline.renderBlock(blockState, pos, this.region, translation, builder, true);
                    }
                }
            }
        }

        BufferBuilder.State translucentBufferState = null;

        for (RenderLayer layer : RenderLayer.getBlockLayers()) {
            BufferBuilder builder = buffers.get(layer);

            if (!builder.isBuilding()) {
                continue;
            }

            if (((ChunkMeshBuilder) builder).isEmpty()) {
                continue;
            }

            if (layer == RenderLayer.getTranslucent()) {
                builder.sortQuads((float) this.camera.x - (float) from.getX(),
                        (float) this.camera.y - (float) from.getY(),
                        (float) this.camera.z - (float) from.getZ());

                translucentBufferState = builder.popState();
            }

            info.presentLayers.add(layer);
        }

        info.occlusionGraph = occlusionDataBuilder.build();
        info.translucentBufferState = translucentBufferState;

        return info;
    }

    public static class Result extends ChunkRenderUploadTask {
        private final ChunkRender<?> chunkRender;
        private final ChunkMeshInfo meshInfo;
        private final Object2ObjectMap<RenderLayer, BufferUploadData> uploads;

        public Result(ChunkRender<?> chunkRender, ChunkMeshInfo meshInfo, Object2ObjectMap<RenderLayer, BufferUploadData> uploads) {
            this.chunkRender = chunkRender;
            this.meshInfo = meshInfo;
            this.uploads = uploads;
        }

        @Override
        public void performUpload() {
            if (this.chunkRender.isInvalid()) {
                return;
            }

            this.chunkRender.upload(this.meshInfo, this.uploads);
            this.chunkRender.finishRebuild();
        }
    }
}
