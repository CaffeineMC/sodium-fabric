package me.jellysquid.mods.sodium.client.render.renderer;

import me.jellysquid.mods.sodium.client.model.light.QuadLighter;
import me.jellysquid.mods.sodium.client.model.light.cache.SlicedLightDataCache;
import me.jellysquid.mods.sodium.client.model.light.smooth.SmoothQuadLighter;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.occlusion.BlockOcclusionCache;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import me.jellysquid.mods.sodium.client.world.cloned.ChunkRenderContext;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.function.Consumer;

public class TerrainRenderContext extends RenderContextBase implements RenderContext {
    private final WorldSlice worldSlice;
    private final SlicedLightDataCache lightDataCache;
    private final TerrainBlockRenderInfo blockRenderInfo;

    private final Consumer<Mesh> meshConsumer;
    private final Consumer<BakedModel> fallbackConsumer;

    private final TerrainRenderer terrainRenderer;

    public TerrainRenderContext(World world, ChunkBuildBuffers buffers) {
        this.worldSlice = new WorldSlice(world);

        this.blockRenderInfo = new TerrainBlockRenderInfo(new BlockOcclusionCache());
        this.blockRenderInfo.setBlockView(this.worldSlice);

        this.lightDataCache = new SlicedLightDataCache(this.worldSlice);

        QuadLighter lighter = new SmoothQuadLighter(this.lightDataCache, this.blockRenderInfo);

        this.terrainRenderer = new TerrainRenderer(this.blockRenderInfo, buffers, lighter, this::transform);
        this.meshConsumer = this.terrainRenderer::acceptFabricMesh;
        this.fallbackConsumer = this.terrainRenderer::renderVanillaModel;
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

    public void prepare(ChunkRenderContext context) {
        this.worldSlice.prepare(context);
        this.lightDataCache.prepare(context.getOrigin());
        this.blockRenderInfo.setChunkId(context.getRelativeChunkIndex());
    }

    public boolean renderBlock(BlockState state, BlockPos.Mutable pos, int detailLevel) {
        this.blockRenderInfo.prepareForBlock(state, pos, true);

        FabricBakedModel model = (FabricBakedModel) MinecraftClient.getInstance().getBlockRenderManager().getModel(state);
        model.emitBlockQuads(this.blockRenderInfo.blockView, this.blockRenderInfo.blockState, this.blockRenderInfo.blockPos, this.blockRenderInfo.getRandomSupplier(), this);

        return true;
    }

    public boolean renderFluid(FluidState fluidState, BlockPos.Mutable blockPos) {
        return false;
    }
}
