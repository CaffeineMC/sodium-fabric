package net.caffeinemc.mods.sodium.client.fabric;

import com.mojang.blaze3d.vertex.PoseStack;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModel;
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
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.FluidState;
import org.joml.Matrix4f;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class SodiumMultiPlatImpl {
    public static boolean isBlockTransparent(BlockState block, BlockAndTintGetter level, BlockPos pos, FluidState fluidState) {
        return FluidRenderHandlerRegistry.INSTANCE.isBlockTransparent(block.getBlock());
    }

    public static TextureAtlasSprite findInBlockAtlas(float u, float v) {
        return SpriteFinderCache.forBlockAtlas().find(u, v);
    }

    public static Object getRenderData(Level level, BoundingBox pos, BlockEntity value) {
        if (value == null) {
            return null;
        }
        return value.getRenderData();
    }

    public static Object getModelData(Object o, BlockPos pos) {
        return null;
    }

    public static boolean isFlawlessFramesActive() {
        return FlawlessFrames.isActive();
    }

    public static Path getGameDir() {
        return FabricLoader.getInstance().getGameDir();
    }

    public static Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    public static boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    public static Iterable<RenderType> getMaterials(BlockAndTintGetter level, BakedModel model, BlockState state, BlockPos pos, RandomSource random, Object modelData) {
        return Collections.singleton(ItemBlockRenderTypes.getChunkRenderType(state));
    }

    public static List<BakedQuad> getQuads(BlockAndTintGetter level, BlockPos pos, BakedModel model, BlockState state, Direction face, RandomSource random, RenderType renderType, Object modelData) {
        return model.getQuads(state, face, random);
    }

    public static Object getEmptyModelData() {
        return null;
    }

    public static void runChunkLayerEvents(RenderType renderType, LevelRenderer levelRenderer, PoseStack poseStack, Matrix4f projectionMatrix, int renderTick, Camera camera, Frustum frustum) {

    }

    public static boolean shouldSkipRender(BlockGetter level, BlockState selfState, BlockState otherState, BlockPos selfPos, Direction facing) {
        return false;
    }
}
