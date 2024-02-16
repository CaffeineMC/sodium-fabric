package net.caffeinemc.mods.sodium.client.fabric;

import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

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

    public static Object getRenderData(BlockEntity value) {
        return value.getRenderData();
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

    public static Iterable<RenderType> getMaterials(BlockRenderContext ctx, RandomSource randomSource) {
        return Collections.singleton(ItemBlockRenderTypes.getChunkRenderType(ctx.state()));
    }

    public static List<BakedQuad> getQuads(BlockRenderContext ctx, Direction face, RandomSource random, RenderType renderType) {
        return ctx.model().getQuads(ctx.state(), face, random);
    }
}
