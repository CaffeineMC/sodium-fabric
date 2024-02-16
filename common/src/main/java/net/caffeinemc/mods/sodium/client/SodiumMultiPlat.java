package net.caffeinemc.mods.sodium.client;

import com.google.gson.internal.$Gson$Types;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

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
    public static Object getRenderData(BlockEntity value) {
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
    public static Iterable<RenderType> getMaterials(BlockRenderContext ctx, RandomSource random) {
        throw new AssertionError("Platform specific code meant to be called!");
    }

    @ExpectPlatform
    public static List<BakedQuad> getQuads(BlockRenderContext ctx, Direction face, RandomSource random, RenderType renderType) {
        throw new AssertionError("Platform specific code meant to be called!");
    }
}
