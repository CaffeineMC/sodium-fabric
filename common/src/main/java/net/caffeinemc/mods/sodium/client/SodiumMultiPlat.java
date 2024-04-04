package net.caffeinemc.mods.sodium.client;

import com.google.gson.internal.$Gson$Types;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.minecraft.client.Camera;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.FluidState;
import org.joml.Matrix4f;

import java.nio.file.Path;
import java.util.List;

public class SodiumMultiPlat {
    @ExpectPlatform
    public static boolean isBlockTransparent(BlockState block, BlockAndTintGetter level, BlockPos pos, FluidState fluidState) {
        throw new AssertionError("Platform specific code meant to be called!");
    }

    @ExpectPlatform
    public static TextureAtlasSprite findInBlockAtlas(float u, float v) {
        throw new AssertionError("Platform specific code meant to be called!");
    }

    @ExpectPlatform
    public static Object getRenderData(Level level, BoundingBox pos, BlockEntity value) {
        throw new AssertionError("Platform specific code meant to be called!");
    }

    @ExpectPlatform
    public static boolean isFlawlessFramesActive() {
        throw new AssertionError("Platform specific code meant to be called!");
    }

    @ExpectPlatform
    public static Path getGameDir() {
        throw new AssertionError("Platform specific path!");
    }

    @ExpectPlatform
    public static Path getConfigDir() {
        throw new AssertionError("Platform specific path!");
    }

    @ExpectPlatform
    public static boolean isDevelopmentEnvironment() {
        throw new AssertionError("Platform specific code meant to be called!");
    }

    @ExpectPlatform
    public static Iterable<RenderType> getMaterials(BlockAndTintGetter level, BakedModel model, BlockState state, BlockPos pos, RandomSource random, Object modelData) {
        throw new AssertionError("Platform specific code meant to be called!");
    }

    @ExpectPlatform
    public static List<BakedQuad> getQuads(BlockAndTintGetter level, BlockPos pos, BakedModel model, BlockState state, Direction face, RandomSource random, RenderType renderType, Object modelData) {
        throw new AssertionError("Platform specific code meant to be called!");
    }

    @ExpectPlatform
    public static Object getModelData(Object o, BlockPos pos) {
        throw new AssertionError("Platform specific code meant to be called!");
    }

    @ExpectPlatform
    public static Object getEmptyModelData() {
        throw new AssertionError("Platform specific code meant to be called!");
    }

    @ExpectPlatform
    public static void runChunkLayerEvents(RenderType renderType, LevelRenderer levelRenderer, PoseStack poseStack, Matrix4f projectionMatrix, int renderTick, Camera camera, Frustum frustum) {
        throw new AssertionError("Platform specific code meant to be called!");
    }

    @ExpectPlatform
    public static boolean shouldSkipRender(BlockGetter level, BlockState selfState, BlockState otherState, BlockPos selfPos, Direction facing) {
        throw new AssertionError("Platform specific code meant to be called!");
    }
}
