package me.jellysquid.mods.sodium.client.render.pipeline;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.light.LightPipeline;
import me.jellysquid.mods.sodium.client.render.light.LightResult;
import me.jellysquid.mods.sodium.client.render.model.quad.*;
import me.jellysquid.mods.sodium.client.render.model.quad.blender.BilinearVertexColorBlender;
import me.jellysquid.mods.sodium.client.render.model.quad.blender.FlatVertexColorBlender;
import me.jellysquid.mods.sodium.client.render.model.quad.blender.VertexColorBlender;
import me.jellysquid.mods.sodium.client.render.occlusion.BlockOcclusionCache;
import me.jellysquid.mods.sodium.client.util.ColorUtil;
import me.jellysquid.mods.sodium.client.util.QuadUtil;
import me.jellysquid.mods.sodium.client.world.biome.BlockColorsExtended;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.block.BlockColorProvider;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockRenderView;

import java.util.List;
import java.util.Random;

public class BlockRenderPipeline {
    private final BlockColorsExtended blockColors;

    private final LightPipeline smoothLightPipeline;
    private final LightPipeline flatLightPipeline;

    private final BlockOcclusionCache occlusionCache;

    private final ModelQuad cachedQuad = new ModelQuad();
    private final LightResult cachedLightResult = new LightResult();

    private final VertexColorBlender colorBlender;

    public BlockRenderPipeline(MinecraftClient client, LightPipeline smoothLightPipeline, LightPipeline flatLightPipeline) {
        this.blockColors = (BlockColorsExtended) client.getBlockColorMap();
        this.smoothLightPipeline = smoothLightPipeline;
        this.flatLightPipeline = flatLightPipeline;

        this.occlusionCache = new BlockOcclusionCache();

        int biomeBlendRadius = SodiumClientMod.options().quality.biomeBlendDistance;

        if (biomeBlendRadius <= 0) {
            this.colorBlender = new FlatVertexColorBlender();
        } else {
            this.colorBlender = new BilinearVertexColorBlender();
        }
    }

    public boolean renderModel(ChunkRenderData.Builder meshInfo, BlockRenderView world, BakedModel model, BlockState state, BlockPos pos, ModelQuadConsumer builder, boolean cull, Random random, long seed) {
        LightPipeline lighter = this.getLightPipeline(state, model);
        lighter.reset();

        Vec3d offset = state.getOffsetPos(world, pos);

        boolean rendered = false;

        for (Direction dir : DirectionUtil.ALL_DIRECTIONS) {
            random.setSeed(seed);

            List<BakedQuad> sided = model.getQuads(state, dir, random);

            if (sided.isEmpty()) {
                continue;
            }

            if (!cull || this.occlusionCache.shouldDrawSide(state, world, pos, dir)) {
                this.renderQuadList(meshInfo, world, state, pos, lighter, offset, builder, sided);

                rendered = true;
            }
        }

        random.setSeed(seed);

        List<BakedQuad> all = model.getQuads(state, null, random);

        if (!all.isEmpty()) {
            this.renderQuadList(meshInfo, world, state, pos, lighter, offset, builder, all);

            rendered = true;
        }

        return rendered;
    }

    private void renderQuadList(ChunkRenderData.Builder meshInfo, BlockRenderView world, BlockState state, BlockPos pos, LightPipeline lighter, Vec3d offset, ModelQuadConsumer builder, List<BakedQuad> quads) {
        BlockColorProvider colorizer = null;

        for (BakedQuad quad : quads) {
            LightResult light = this.cachedLightResult;
            lighter.apply((ModelQuadView) quad, pos, light, quad.getFace());

            if (quad.hasColor() && colorizer == null) {
                colorizer = this.blockColors.getColorProvider(state);
            }

            this.renderQuad(world, state, pos, builder, offset, colorizer, quad, light.br, light.lm);

            if (meshInfo != null) {
                meshInfo.addSprite(((ModelQuadView) quad).getSprite());
            }
        }
    }

    private void renderQuad(BlockRenderView world, BlockState state, BlockPos pos, ModelQuadConsumer consumer, Vec3d offset, BlockColorProvider colorProvider, BakedQuad bakedQuad, float[] brightnesses, int[] lights) {
        ModelQuadView quad = (ModelQuadView) bakedQuad;
        ModelQuadOrder order = ModelQuadOrder.orderOf(brightnesses);
        ModelQuadViewMutable copy = this.cachedQuad;

        int norm = QuadUtil.getNormal(bakedQuad.getFace());
        boolean hasColor = bakedQuad.hasColor();

        for (int dstIndex = 0; dstIndex < 4; dstIndex++) {
            int srcIndex = order.getVertexIndex(dstIndex);

            float x = quad.getX(srcIndex) + (float) offset.getX();
            float y = quad.getY(srcIndex) + (float) offset.getY();
            float z = quad.getZ(srcIndex) + (float) offset.getZ();

            copy.setX(dstIndex, x);
            copy.setY(dstIndex, y);
            copy.setZ(dstIndex, z);

            float br = brightnesses[srcIndex];
            int color = quad.getColor(srcIndex);

            if (hasColor) {
                copy.setColor(dstIndex, this.colorBlender.getColor(colorProvider, state, world, color, bakedQuad.getColorIndex(), x, z, pos, br));
            } else {
                copy.setColor(dstIndex, ColorUtil.mulPackedRGB(color, br, br, br));
            }

            copy.setTexU(dstIndex, quad.getTexU(srcIndex));
            copy.setTexV(dstIndex, quad.getTexV(srcIndex));

            copy.setLight(dstIndex, lights[srcIndex]);
            copy.setNormal(dstIndex, norm);
        }

        consumer.write(copy);
    }

    private LightPipeline getLightPipeline(BlockState state, BakedModel model) {
        boolean smooth = MinecraftClient.isAmbientOcclusionEnabled() && (state.getLuminance() == 0) && model.useAmbientOcclusion();

        return smooth ? this.smoothLightPipeline : this.flatLightPipeline;
    }
}
