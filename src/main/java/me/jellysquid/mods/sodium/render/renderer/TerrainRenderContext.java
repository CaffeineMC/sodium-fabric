package me.jellysquid.mods.sodium.render.renderer;

import me.jellysquid.mods.sodium.model.light.QuadLighter;
import me.jellysquid.mods.sodium.model.light.cache.SlicedLightDataCache;
import me.jellysquid.mods.sodium.model.light.smooth.SmoothQuadLighter;
import me.jellysquid.mods.sodium.model.vertex.type.ChunkVertexType;
import me.jellysquid.mods.sodium.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.render.chunk.data.BuiltChunkMesh;
import me.jellysquid.mods.sodium.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.render.occlusion.BlockOcclusionCache;
import me.jellysquid.mods.sodium.render.FluidRenderer;
import me.jellysquid.mods.sodium.render.renderer.transforms.ModelOffsetTransform;
import me.jellysquid.mods.sodium.world.WorldSlice;
import me.jellysquid.mods.sodium.world.cloned.ChunkRenderContext;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Map;
import java.util.function.Consumer;

public class TerrainRenderContext extends RenderContextBase implements RenderContext {
    private final WorldSlice worldSlice;

    private final SlicedLightDataCache lightDataCache;
    private final TerrainBlockRenderInfo blockRenderInfo;

    private final ChunkBuildBuffers buffers;

    private final Consumer<Mesh> meshConsumer;
    private final Consumer<BakedModel> fallbackConsumer;

    private final TerrainRenderer terrainRenderer;
    private final FluidRenderer fluidRenderer;

    public TerrainRenderContext(World world, ChunkVertexType vertexType) {
        this.worldSlice = new WorldSlice(world);

        this.blockRenderInfo = new TerrainBlockRenderInfo(new BlockOcclusionCache());
        this.blockRenderInfo.setBlockView(this.worldSlice);

        this.buffers = new ChunkBuildBuffers(vertexType);
        this.lightDataCache = new SlicedLightDataCache(this.worldSlice);

        QuadLighter lighter = new SmoothQuadLighter(this.lightDataCache, this.blockRenderInfo);

        this.terrainRenderer = new TerrainRenderer(this.blockRenderInfo, this.buffers, lighter, this::transform);
        this.meshConsumer = this.terrainRenderer::acceptFabricMesh;
        this.fallbackConsumer = this.terrainRenderer::renderVanillaModel;
        this.fluidRenderer = new FluidRenderer(this.terrainRenderer.biomeColorBlender);
    }

    @Override
    public Consumer<Mesh> meshConsumer() {
        return this.meshConsumer;
    }

    @Override
    public Consumer<BakedModel> fallbackConsumer() {
        return this.fallbackConsumer;
    }

    @Override
    public QuadEmitter getEmitter() {
        return this.terrainRenderer.getEmitter();
    }

    public WorldSlice getWorldSlice() {
        return this.worldSlice;
    }

    public void prepare(ChunkRenderContext context, ChunkRenderData.Builder renderData) {
        this.worldSlice.prepare(context);
        this.lightDataCache.prepare(context.getOrigin());
        this.blockRenderInfo.setChunkId(context.getRelativeChunkIndex());
        this.buffers.prepare(renderData);
    }

    public void release() {
        this.buffers.release();
    }

    private final ModelOffsetTransform modelOffsetTransform = new ModelOffsetTransform();

    public void renderBlock(BlockState state, BlockPos.Mutable pos, int detailLevel) {
        this.blockRenderInfo.prepareForBlock(state, pos, true);

        if (this.blockRenderInfo.blockState.getRenderType() == BlockRenderType.MODEL) {
            Vec3d offset = this.blockRenderInfo.blockState.getModelOffset(this.blockRenderInfo.blockView, this.blockRenderInfo.blockPos);
            boolean hasOffset = offset != Vec3d.ZERO;

            if (hasOffset) {
                this.pushTransform(this.modelOffsetTransform.prepare(offset));
            }

            FabricBakedModel model = (FabricBakedModel) MinecraftClient.getInstance().getBlockRenderManager().getModel(state);
            model.emitBlockQuads(this.blockRenderInfo.blockView, this.blockRenderInfo.blockState, this.blockRenderInfo.blockPos, this.blockRenderInfo.getRandomSupplier(), this);

            if (hasOffset) {
                this.popTransform();
            }
        }

        if (!this.blockRenderInfo.fluidState.isEmpty()) {
            this.fluidRenderer.render(this.blockRenderInfo.blockView, this.blockRenderInfo.fluidState, this.blockRenderInfo.blockPos, this);
        }
    }

    public Map<BlockRenderPass, BuiltChunkMesh> createBakedMeshes() {
        return this.buffers.createMeshes();
    }
}
