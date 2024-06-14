package net.caffeinemc.mods.sodium.fabric;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.client.model.color.ColorProviderRegistry;
import net.caffeinemc.mods.sodium.client.model.light.LightPipelineProvider;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.FluidRenderer;
import net.caffeinemc.mods.sodium.client.services.SodiumPlatformHelpers;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.caffeinemc.mods.sodium.fabric.render.FluidRendererImpl;
import net.caffeinemc.mods.sodium.fabric.render.SpriteFinderCache;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.util.TriState;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.FluidState;
import org.joml.Matrix4f;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class SodiumFabricHelpers implements SodiumPlatformHelpers {
    @Override
    public boolean isBlockTransparent(BlockState block, BlockAndTintGetter level, BlockPos pos, FluidState fluidState) {
        return FluidRenderHandlerRegistry.INSTANCE.isBlockTransparent(block.getBlock());
    }

    @Override
    public Path getGameDir() {
        return FabricLoader.getInstance().getGameDir();
    }

    @Override
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public Object getRenderData(Level level, BoundingBox pos, BlockEntity value) {
        if (value == null) {
            return null;
        }
        return value.getRenderData();
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public boolean isFlawlessFramesActive() {
        return FlawlessFrames.isActive();
    }

    @Override
    public Iterable<RenderType> getMaterials(BlockAndTintGetter level, BakedModel model, BlockState state, BlockPos pos, RandomSource random, Object modelData) {
        return Collections.singleton(ItemBlockRenderTypes.getChunkRenderType(state));
    }

    @Override
    public List<BakedQuad> getQuads(BlockAndTintGetter level, BlockPos pos, BakedModel model, BlockState state, Direction face, RandomSource random, RenderType renderType, Object modelData) {
        return model.getQuads(state, face, random);
    }

    @Override
    public Object getModelData(Object o, BlockPos pos) {
        return null;
    }

    @Override
    public Object getEmptyModelData() {
        return null;
    }

    @Override
    public boolean shouldSkipRender(BlockGetter level, BlockState selfState, BlockState otherState, BlockPos selfPos, Direction facing) {
        return false;
    }

    @Override
    public int getLightEmission(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        return state.getLightEmission();
    }

    @Override
    public boolean shouldCopyRenderData() {
        return true;
    }

    @Override
    public boolean renderFluidFromVanilla() {
        return FluidRendererImpl.renderFromVanilla();
    }

    @Override
    public void runChunkLayerEvents(RenderType renderLayer, LevelRenderer levelRenderer, Matrix4f modelMatrix, Matrix4f projectionMatrix, int ticks, Camera mainCamera, Frustum cullingFrustum) {

    }

    @Override
    public FluidRenderer createPlatformFluidRenderer(ColorProviderRegistry colorRegistry, LightPipelineProvider lightPipelineProvider) {
        return new FluidRendererImpl(colorRegistry, lightPipelineProvider);
    }

    @Override
    public TextureAtlasSprite findInBlockAtlas(float u, float v) {
        return SpriteFinderCache.forBlockAtlas().find(u, v);
    }

    @Override
    public boolean isEarlyLoadingScreenActive() {
        return false;
    }

    @Override
    public Object getProperModelData(BakedModel model, BlockState state, BlockPos pos, LevelSlice slice, Object modelData) {
        return modelData;
    }

    @Override
    public void renderConnectionsInSection(ChunkBuildBuffers buffers, LevelSlice worldSlice, SectionPos position) {

    }

    @Override
    public boolean shouldRenderIE(SectionPos position) {
        return false;
    }

    @Override
    public void renderAdditionalRenderers(List<?> renderers, Function<RenderType, VertexConsumer> typeToConsumer, LevelSlice slice) {
        // Fabric has no concept of additional chunk renderers; everything is handled through FRAPI.
    }

    @Override
    public List<?> getExtraRenderers(Level level, BlockPos origin) {
        return List.of();
    }

    @Override
    public Object getLightManager(LevelChunk chunk, SectionPos pos) {
        return null;
    }

    @Override
    public TriState useAmbientOcclusion(BakedModel model, BlockState state, Object data, RenderType renderType, BlockAndTintGetter level, BlockPos pos) {
        return model.useAmbientOcclusion() ? TriState.DEFAULT : TriState.FALSE;
    }
}
