package net.caffeinemc.mods.sodium.client.services;

import com.mojang.blaze3d.vertex.PoseStack;
import net.caffeinemc.mods.sodium.client.model.color.ColorProviderRegistry;
import net.caffeinemc.mods.sodium.client.model.light.LightPipelineProvider;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.FluidRenderer;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.FluidState;
import org.joml.Matrix4f;

import java.nio.file.Path;
import java.util.List;

public interface SodiumPlatformHelpers {
    public static SodiumPlatformHelpers INSTANCE = Services.load(SodiumPlatformHelpers.class);

    boolean isBlockTransparent(BlockState block, BlockAndTintGetter level, BlockPos pos, FluidState fluidState);

    Path getGameDir();

    Path getConfigDir();

    Object getRenderData(Level level, BoundingBox pos, BlockEntity value);

    boolean isDevelopmentEnvironment();

    boolean isFlawlessFramesActive();

    Iterable<RenderType> getMaterials(BlockAndTintGetter level, BakedModel model, BlockState state, BlockPos pos, RandomSource random, Object modelData);

    List<BakedQuad> getQuads(BlockAndTintGetter level, BlockPos pos, BakedModel model, BlockState state, Direction face, RandomSource random, RenderType renderType, Object modelData);

    Object getModelData(Object o, BlockPos pos);

    Object getEmptyModelData();

    boolean shouldSkipRender(BlockGetter level, BlockState selfState, BlockState otherState, BlockPos selfPos, Direction facing);

    int getLightEmission(BlockState state, BlockAndTintGetter level, BlockPos pos);

    boolean shouldCopyRenderData(); // True on Fabric!

    boolean renderFluidFromVanilla();

    void runChunkLayerEvents(RenderType renderLayer, LevelRenderer levelRenderer, PoseStack matrices, Matrix4f matrix, int ticks, Camera mainCamera, Frustum cullingFrustum);

    FluidRenderer createPlatformFluidRenderer(ColorProviderRegistry colorRegistry, LightPipelineProvider lightPipelineProvider);

    TextureAtlasSprite findInBlockAtlas(float texU, float texV);

    boolean isEarlyLoadingScreenActive();

    Object getProperModelData(BakedModel model, BlockState state, BlockPos pos, LevelSlice slice, Object modelData);
}
