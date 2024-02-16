package net.caffeinemc.mods.sodium.client.neoforge;

import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials;
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
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.nio.file.Path;
import java.util.List;

public class SodiumMultiPlatImpl {
    public static boolean isBlockTransparent(BlockState block, BlockAndTintGetter level, BlockPos pos, FluidState fluidState) {
        return block.shouldDisplayFluidOverlay(level, pos, fluidState);
    }

    public static TextureAtlasSprite findInBlockAtlas(float u, float v) {
        // TODO
        //return SpriteFinderCache.forBlockAtlas().find(u, v);
        return null;
    }

    public static Object getRenderData(BlockEntity value) {
        return null;
    }

    public static boolean isFlawlessFramesActive() {
        return false;
    }

    public static Path getGameDir() {
        return FMLPaths.GAMEDIR.get();
    }

    public static Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    public static boolean isDevelopmentEnvironment() {
        return !FMLLoader.isProduction();
    }

    public static Iterable<RenderType> getMaterials(BlockRenderContext ctx, RandomSource random) {
        return ctx.model().getRenderTypes(ctx.state(), random, ctx.model().getModelData(ctx.slice(), ctx.pos(), ctx.state(), ModelData.EMPTY));
    }

    public static List<BakedQuad> getQuads(BlockRenderContext ctx, Direction face, RandomSource random, RenderType renderType) {
        return ctx.model().getQuads(ctx.state(), face, random, ctx.model().getModelData(ctx.slice(), ctx.pos(), ctx.state(), ModelData.EMPTY), renderType);
    }
}
