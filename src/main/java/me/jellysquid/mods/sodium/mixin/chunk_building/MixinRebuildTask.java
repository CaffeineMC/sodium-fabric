package me.jellysquid.mods.sodium.mixin.chunk_building;

import com.google.common.collect.Sets;
import me.jellysquid.mods.sodium.client.render.pipeline.ChunkRenderPipeline;
import me.jellysquid.mods.sodium.client.render.pipeline.ExtendedChunkData;
import me.jellysquid.mods.sodium.client.render.mesh.ChunkMeshBuilder;
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
import net.minecraft.client.render.chunk.BlockBufferBuilderStorage;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.chunk.ChunkOcclusionDataBuilder;
import net.minecraft.client.render.chunk.ChunkRendererRegion;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(targets = "net/minecraft/client/render/chunk/ChunkBuilder$BuiltChunk$RebuildTask")
public abstract class MixinRebuildTask {
    @Shadow(aliases = "field_20839")
    private ChunkBuilder.BuiltChunk parent;

    @Shadow
    protected ChunkRendererRegion region;

    @Shadow
    protected abstract <E extends BlockEntity> void addBlockEntity(ChunkBuilder.ChunkData data, Set<BlockEntity> blockEntities, E blockEntity);

    private ChunkRenderPipeline pipeline;

    @SuppressWarnings("InvalidInjectorMethodSignature")
    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(ChunkBuilder.BuiltChunk builtChunk, double d, ChunkRendererRegion world, CallbackInfo ci) {
        this.pipeline = new ChunkRenderPipeline(MinecraftClient.getInstance(), world, builtChunk.getOrigin());
    }

    /**
     * @author JellySquid
     */
    @Overwrite
    private Set<BlockEntity> render(float cameraX, float cameraY, float cameraZ, ChunkBuilder.ChunkData data, BlockBufferBuilderStorage buffers) {
        ExtendedChunkData edata = (ExtendedChunkData) data;

        BlockPos from = this.parent.getOrigin().toImmutable();
        BlockPos to = from.add(15, 15, 15);

        ChunkOcclusionDataBuilder occlusion = new ChunkOcclusionDataBuilder();

        Set<BlockEntity> set = Sets.newHashSet();

        ChunkRendererRegion world = this.region;

        Vector3f translation = new Vector3f();

        if (world != null) {
            BlockRenderManager blockRenderManager = MinecraftClient.getInstance().getBlockRenderManager();

            int minX = from.getX();
            int minY = from.getY();
            int minZ = from.getZ();

            int maxX = to.getX();
            int maxY = to.getY();
            int maxZ = to.getZ();

            BlockPos.Mutable pos = new BlockPos.Mutable();

            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int x = minX; x <= maxX; x++) {
                        pos.set(x, y, z);

                        BlockState blockState = world.getBlockState(pos);
                        FluidState fluidState = blockState.getFluidState();

                        Block block = blockState.getBlock();

                        if (blockState.isFullOpaque(world, pos)) {
                            occlusion.markClosed(pos);
                        }

                        if (block.hasBlockEntity()) {
                            BlockEntity entity = world.getBlockEntity(pos, WorldChunk.CreationType.CHECK);

                            if (entity != null) {
                                this.addBlockEntity(data, set, entity);
                            }
                        }

                        if (!fluidState.isEmpty()) {
                            RenderLayer layer = RenderLayers.getFluidLayer(fluidState);

                            BufferBuilder builder = buffers.get(layer);

                            if (!builder.isBuilding()) {
                                builder.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL);

                                edata.getInitializedLayers().add(layer);
                            }

                            blockRenderManager.renderFluid(pos, world, builder, fluidState);
                        }

                        if (blockState.getRenderType() != BlockRenderType.INVISIBLE) {
                            RenderLayer layer = RenderLayers.getBlockLayer(blockState);

                            BufferBuilder builder = buffers.get(layer);

                            if (!builder.isBuilding()) {
                                builder.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL);

                                edata.getInitializedLayers().add(layer);
                            }

                            translation.set(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);

                            this.pipeline.renderBlock(blockState, pos, world, translation, builder, true);
                        }
                    }
                }
            }

            for (RenderLayer layer : edata.getInitializedLayers()) {
                BufferBuilder builder = buffers.get(layer);

                if (layer == RenderLayer.getTranslucent()) {
                    builder.sortQuads(cameraX - (float) from.getX(), cameraY - (float) from.getY(), cameraZ - (float) from.getZ());

                    edata.setTranslucentBufferState(builder.popState());
                }

                builder.end();

                if (((ChunkMeshBuilder) builder).isEmpty()) {
                    continue;
                }

                edata.getNonEmptyLayers().add(layer);
            }


            if (edata.getNonEmptyLayers().size() > 0) {
                edata.markNonEmpty();
            }
        }

        edata.setOcclusionData(occlusion.build());

        this.region = null;
        this.pipeline = null;

        return set;
    }
}

