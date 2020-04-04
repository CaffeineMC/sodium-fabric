package me.jellysquid.mods.sodium.client.render.pipeline;

import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkSlice;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkMeshInfo;
import me.jellysquid.mods.sodium.client.render.light.LightPipeline;
import me.jellysquid.mods.sodium.client.render.light.LightResult;
import me.jellysquid.mods.sodium.client.render.light.flat.FlatLightPipeline;
import me.jellysquid.mods.sodium.client.render.light.smooth.SmoothLightPipeline;
import me.jellysquid.mods.sodium.client.render.mesh.ChunkMeshBuilder;
import me.jellysquid.mods.sodium.client.render.occlusion.BlockOcclusionCache;
import me.jellysquid.mods.sodium.client.render.quad.ModelQuad;
import me.jellysquid.mods.sodium.client.render.quad.ModelQuadOrder;
import me.jellysquid.mods.sodium.client.render.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.render.quad.ModelQuadViewMutable;
import me.jellysquid.mods.sodium.client.util.ColorUtil;
import me.jellysquid.mods.sodium.client.util.QuadUtil;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockRenderView;

import java.util.List;
import java.util.Random;

public class ChunkBlockRenderPipeline {
    private final BlockColors colorMap;

    private final LightPipeline smoothLightPipeline;
    private final LightPipeline flatLightPipeline;

    private final BlockOcclusionCache occlusionCache;

    private final Int2IntFunction cachedColors;

    private final ModelQuad cachedQuad = new ModelQuad();
    private final LightResult cachedLightResult = new LightResult();

    public ChunkBlockRenderPipeline(MinecraftClient client, ChunkSlice world) {
        this.colorMap = client.getBlockColorMap();

        this.smoothLightPipeline = new SmoothLightPipeline(world.getLightDataCache());
        this.flatLightPipeline = new FlatLightPipeline(world.getLightDataCache());

        this.occlusionCache = new BlockOcclusionCache();

        this.cachedColors = new Int2IntArrayMap();
        this.cachedColors.defaultReturnValue(Integer.MAX_VALUE);
    }

    public boolean renderModel(ChunkMeshInfo.Builder meshInfo, BlockRenderView world, BakedModel model, BlockState state, BlockPos pos, Vector3f translation, VertexConsumer builder, boolean cull, Random random, long seed) {
        LightPipeline lighter = this.getLightPipeline(state, model);
        lighter.reset();

        this.cachedColors.clear();

        Vec3d blockOffset = state.getOffsetPos(world, pos);
        translation.add((float) blockOffset.getX(), (float) blockOffset.getY(), (float) blockOffset.getZ());

        boolean rendered = false;

        for (Direction dir : DirectionUtil.ALL_DIRECTIONS) {
            random.setSeed(seed);

            List<BakedQuad> sided = model.getQuads(state, dir, random);

            if (sided.isEmpty()) {
                continue;
            }

            if (!cull || this.occlusionCache.shouldDrawSide(state, world, pos, dir)) {
                this.renderQuadList(meshInfo, world, state, pos, lighter, translation, builder, sided, dir);

                rendered = true;
            }
        }

        random.setSeed(seed);

        List<BakedQuad> all = model.getQuads(state, null, random);

        if (!all.isEmpty()) {
            this.renderQuadList(meshInfo, world, state, pos, lighter, translation, builder, all, null);

            rendered = true;
        }

        return rendered;
    }

    private void renderQuadList(ChunkMeshInfo.Builder meshInfo, BlockRenderView world, BlockState state, BlockPos pos, LightPipeline lighter, Vector3f translation, VertexConsumer builder, List<BakedQuad> quads, Direction dir) {
        for (BakedQuad quad : quads) {
            LightResult light = this.cachedLightResult;
            lighter.apply((ModelQuadView) quad, pos, light);

            this.renderQuad(world, state, pos, builder, translation, (ModelQuadView) quad, light.br, light.lm);

            meshInfo.addSprite(((ModelQuadView) quad).getSprite());
        }
    }

    private void renderQuad(BlockRenderView world, BlockState state, BlockPos pos, VertexConsumer builder, Vector3f translation, ModelQuadView quad, float[] brightnesses, int[] lights) {
        int color = this.getQuadColor(quad, world, state, pos);

        float r, g, b;

        if (color != Integer.MIN_VALUE) {
            r = ColorUtil.normalize(ColorUtil.unpackColorR(color));
            g = ColorUtil.normalize(ColorUtil.unpackColorG(color));
            b = ColorUtil.normalize(ColorUtil.unpackColorB(color));
        } else {
            r = 1.0f;
            g = 1.0f;
            b = 1.0f;
        }

        ModelQuadOrder order = ModelQuadOrder.orderOf(brightnesses);
        ModelQuadViewMutable copy = this.cachedQuad;

        int norm = QuadUtil.getNormal(quad.getFacing());

        for (int i = 0; i < 4; i++) {
            int o = order.getVertexIndex(i);

            copy.setX(i, quad.getX(o) + translation.getX());
            copy.setY(i, quad.getY(o) + translation.getY());
            copy.setZ(i, quad.getZ(o) + translation.getZ());

            float br = brightnesses[o];

            if (color != Integer.MIN_VALUE) {
                copy.setColor(i, ColorUtil.mulPackedRGB(quad.getColor(o), r * br, g * br, b * br));
            } else {
                copy.setColor(i, ColorUtil.mulPacked(quad.getColor(o), br));
            }

            copy.setTexU(i, quad.getTexU(o));
            copy.setTexV(i, quad.getTexV(o));

            copy.setLight(i, lights[o]);
            copy.setNormal(i, norm);
        }

        ((ChunkMeshBuilder) builder).write(copy);
    }

    private int getQuadColor(ModelQuadView quad, BlockRenderView world, BlockState state, BlockPos pos) {
        if (quad.hasColorIndex()) {
            int color = this.cachedColors.get(quad.getColorIndex());

            if (color == Integer.MAX_VALUE) {
                this.cachedColors.put(quad.getColorIndex(), color = this.colorMap.getColor(state, world, pos, quad.getColorIndex()));
            }

            return color;
        }

        return Integer.MIN_VALUE;
    }

    private LightPipeline getLightPipeline(BlockState state, BakedModel model) {
        boolean smooth = MinecraftClient.isAmbientOcclusionEnabled() && state.getLuminance() == 0 && model.useAmbientOcclusion();

        return smooth ? this.smoothLightPipeline : this.flatLightPipeline;
    }
}
