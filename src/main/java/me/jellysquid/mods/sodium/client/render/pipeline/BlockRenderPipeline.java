package me.jellysquid.mods.sodium.client.render.pipeline;

import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.light.LightPipeline;
import me.jellysquid.mods.sodium.client.render.light.LightResult;
import me.jellysquid.mods.sodium.client.render.light.flat.FlatLightPipeline;
import me.jellysquid.mods.sodium.client.render.light.smooth.SmoothLightPipeline;
import me.jellysquid.mods.sodium.client.render.model.quad.*;
import me.jellysquid.mods.sodium.client.render.occlusion.BlockOcclusionCache;
import me.jellysquid.mods.sodium.client.util.ColorUtil;
import me.jellysquid.mods.sodium.client.util.QuadUtil;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockRenderView;

import java.util.List;
import java.util.Random;

public class BlockRenderPipeline {
    private final BlockColors colorMap;

    private final SmoothLightPipeline smoothLightPipeline;
    private final FlatLightPipeline flatLightPipeline;

    private final BlockOcclusionCache occlusionCache;

    private final ModelQuad cachedQuad = new ModelQuad();
    private final LightResult cachedLightResult = new LightResult();
    private final Int2IntArrayMap cachedColors = new Int2IntArrayMap();

    public BlockRenderPipeline(MinecraftClient client, SmoothLightPipeline smoothLightPipeline, FlatLightPipeline flatLightPipeline) {
        this.colorMap = client.getBlockColorMap();
        this.smoothLightPipeline = smoothLightPipeline;
        this.flatLightPipeline = flatLightPipeline;

        this.occlusionCache = new BlockOcclusionCache();
        this.cachedColors.defaultReturnValue(Integer.MIN_VALUE);
    }

    public boolean renderModel(ChunkRenderData.Builder meshInfo, BlockRenderView world, BakedModel model, BlockState state, BlockPos pos, ModelQuadTransformer quadTransformer, VertexConsumer builder, boolean cull, Random random, long seed) {
        LightPipeline lighter = this.getLightPipeline(state, model);
        lighter.reset();

        this.cachedColors.clear();

        Vec3d offset = state.getOffsetPos(world, pos);

        boolean rendered = false;

        for (Direction dir : DirectionUtil.ALL_DIRECTIONS) {
            random.setSeed(seed);

            List<BakedQuad> sided = model.getQuads(state, dir, random);

            if (sided.isEmpty()) {
                continue;
            }

            if (!cull || this.occlusionCache.shouldDrawSide(state, world, pos, dir)) {
                this.renderQuadList(meshInfo, world, state, pos, lighter, quadTransformer, offset, builder, sided);

                rendered = true;
            }
        }

        random.setSeed(seed);

        List<BakedQuad> all = model.getQuads(state, null, random);

        if (!all.isEmpty()) {
            this.renderQuadList(meshInfo, world, state, pos, lighter, quadTransformer, offset, builder, all);

            rendered = true;
        }

        return rendered;
    }

    private void renderQuadList(ChunkRenderData.Builder meshInfo, BlockRenderView world, BlockState state, BlockPos pos, LightPipeline lighter, ModelQuadTransformer quadTransformer, Vec3d offset, VertexConsumer builder, List<BakedQuad> quads) {
        for (BakedQuad quad : quads) {
            LightResult light = this.cachedLightResult;
            lighter.apply((ModelQuadView) quad, pos, light, quad.getFace());

            this.renderQuad(world, state, pos, builder, quadTransformer, offset, quad, light.br, light.lm);

            if (meshInfo != null) {
                meshInfo.addSprite(((ModelQuadView) quad).getSprite());
            }
        }
    }

    private void renderQuad(BlockRenderView world, BlockState state, BlockPos pos, VertexConsumer consumer, ModelQuadTransformer quadTransformer, Vec3d offset, BakedQuad bakedQuad, float[] brightnesses, int[] lights) {
        float r, g, b;

        if (bakedQuad.hasColor()) {
            int color = this.getQuadColor(state, world, pos, bakedQuad.getColorIndex());

            r = ColorUtil.normalize(ColorUtil.unpackColorR(color));
            g = ColorUtil.normalize(ColorUtil.unpackColorG(color));
            b = ColorUtil.normalize(ColorUtil.unpackColorB(color));
        } else {
            r = 1.0f;
            g = 1.0f;
            b = 1.0f;
        }

        ModelQuadView quad = (ModelQuadView) bakedQuad;
        ModelQuadOrder order = ModelQuadOrder.orderOf(brightnesses);
        ModelQuadViewMutable copy = this.cachedQuad;

        int norm = QuadUtil.getNormal(bakedQuad.getFace());

        for (int dstIndex = 0; dstIndex < 4; dstIndex++) {
            int srcIndex = order.getVertexIndex(dstIndex);

            copy.setX(dstIndex, quad.getX(srcIndex) + (float) offset.getX());
            copy.setY(dstIndex, quad.getY(srcIndex) + (float) offset.getY());
            copy.setZ(dstIndex, quad.getZ(srcIndex) + (float) offset.getZ());

            float br = brightnesses[srcIndex];
            copy.setColor(dstIndex, ColorUtil.mulPackedRGB(quad.getColor(srcIndex), r * br, g * br, b * br));

            copy.setTexU(dstIndex, quad.getTexU(srcIndex));
            copy.setTexV(dstIndex, quad.getTexV(srcIndex));

            copy.setLight(dstIndex, lights[srcIndex]);
            copy.setNormal(dstIndex, norm);
        }

        quadTransformer.transform(copy);

        writeQuad(consumer, copy);
    }

    private static void writeQuad(VertexConsumer consumer, ModelQuadViewMutable quad) {
        if (consumer instanceof ModelQuadConsumer) {
            ((ModelQuadConsumer) consumer).write(quad);
        } else {
            for (int i = 0; i < 4; i++) {
                int color = quad.getColor(i);

                float r = ColorUtil.normalize(ColorUtil.unpackColorR(color));
                float g = ColorUtil.normalize(ColorUtil.unpackColorG(color));
                float b = ColorUtil.normalize(ColorUtil.unpackColorB(color));
                float a = ColorUtil.normalize(ColorUtil.unpackColorA(color));

                int light = quad.getLight(i);
                int norm = quad.getNormal(i);

                float normX = QuadUtil.unpackNormalX(norm);
                float normY = QuadUtil.unpackNormalY(norm);
                float normZ = QuadUtil.unpackNormalZ(norm);

                consumer.vertex(quad.getX(i), quad.getY(i), quad.getZ(i), r, g, b, a, quad.getTexU(i), quad.getTexV(i), light, OverlayTexture.DEFAULT_UV, normX, normY, normZ);
            }
        }
    }

    private int getQuadColor(BlockState state, BlockRenderView world, BlockPos pos, int colorIndex) {
        int color = this.cachedColors.get(colorIndex);

        if (color == Integer.MIN_VALUE) {
            this.cachedColors.put(colorIndex, color = this.colorMap.getColor(state, world, pos, colorIndex));
        }

        return color;
    }

    private LightPipeline getLightPipeline(BlockState state, BakedModel model) {
        boolean smooth = MinecraftClient.isAmbientOcclusionEnabled() && state.getLuminance() == 0 && model.useAmbientOcclusion();

        return smooth ? this.smoothLightPipeline : this.flatLightPipeline;
    }
}
