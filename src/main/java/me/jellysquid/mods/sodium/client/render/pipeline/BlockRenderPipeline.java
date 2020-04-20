package me.jellysquid.mods.sodium.client.render.pipeline;

import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.light.LightPipeline;
import me.jellysquid.mods.sodium.client.render.light.LightResult;
import me.jellysquid.mods.sodium.client.render.model.quad.*;
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
    private final Int2IntArrayMap cachedColors = new Int2IntArrayMap();

    public BlockRenderPipeline(MinecraftClient client, LightPipeline smoothLightPipeline, LightPipeline flatLightPipeline) {
        this.blockColors = (BlockColorsExtended) client.getBlockColorMap();
        this.smoothLightPipeline = smoothLightPipeline;
        this.flatLightPipeline = flatLightPipeline;

        this.occlusionCache = new BlockOcclusionCache();
        this.cachedColors.defaultReturnValue(Integer.MIN_VALUE);
    }

    public boolean renderModel(ChunkRenderData.Builder meshInfo, BlockRenderView world, BakedModel model, BlockState state, BlockPos pos, ModelQuadConsumer builder, boolean cull, Random random, long seed) {
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
                copy.setColor(dstIndex, this.getColor(colorProvider, state, world, color, bakedQuad.getColorIndex(), x, z, pos, br));
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

    // Bi-linear interpolation for biome colors
    private int getColor(BlockColorProvider colorProvider, BlockState state, BlockRenderView world, int color, int colorIndex, float posX, float posZ, BlockPos origin, float brightness) {
        final BlockPos.Mutable mpos = new BlockPos.Mutable();

        final float x = origin.getX() + posX;
        final float z = origin.getZ() + posZ;

        // Integer component of position vector
        final int intX = (int) x;
        final int intZ = (int) z;

        // Fraction component of position vector
        final float fracX = x - intX;
        final float fracZ = z - intZ;

        // Retrieve the color values for each neighbor
        final int c1 = colorProvider.getColor(state, world, mpos.set(intX, origin.getY(), intZ), colorIndex);
        final int c2 = colorProvider.getColor(state, world, mpos.set(intX, origin.getY(), intZ + 1), colorIndex);
        final int c3 = colorProvider.getColor(state, world, mpos.set(intX + 1, origin.getY(), intZ), colorIndex);
        final int c4 = colorProvider.getColor(state, world, mpos.set(intX + 1, origin.getY(), intZ + 1), colorIndex);;

        // RGB components for each corner's color
        final float c1r = ColorUtil.unpackColorR(c1);
        final float c1g = ColorUtil.unpackColorG(c1);
        final float c1b = ColorUtil.unpackColorB(c1);

        final float c2r = ColorUtil.unpackColorR(c2);
        final float c2g = ColorUtil.unpackColorG(c2);
        final float c2b = ColorUtil.unpackColorB(c2);

        final float c3r = ColorUtil.unpackColorR(c3);
        final float c3g = ColorUtil.unpackColorG(c3);
        final float c3b = ColorUtil.unpackColorB(c3);

        final float c4r = ColorUtil.unpackColorR(c4);
        final float c4g = ColorUtil.unpackColorG(c4);
        final float c4b = ColorUtil.unpackColorB(c4);

        // Compute the final color values across the Z axis
        final float r1r = c1r + ((c2r - c1r) * fracZ);
        final float r1g = c1g + ((c2g - c1g) * fracZ);
        final float r1b = c1b + ((c2b - c1b) * fracZ);

        final float r2r = c3r + ((c4r - c3r) * fracZ);
        final float r2g = c3g + ((c4g - c3g) * fracZ);
        final float r2b = c3b + ((c4b - c3b) * fracZ);

        // Compute the final color values across the X axis
        final float fr = r1r + ((r2r - r1r) * fracX);
        final float fg = r1g + ((r2g - r1g) * fracX);
        final float fb = r1b + ((r2b - r1b) * fracX);

        // Normalize and darken the returned color
        return ColorUtil.mulPackedRGB(color,
                ColorUtil.normalize(fr) * brightness,
                ColorUtil.normalize(fg) * brightness,
                ColorUtil.normalize(fb) * brightness);
    }

    private LightPipeline getLightPipeline(BlockState state, BakedModel model) {
        boolean smooth = MinecraftClient.isAmbientOcclusionEnabled() && (state.getLuminance() == 0) && model.useAmbientOcclusion();

        return smooth ? this.smoothLightPipeline : this.flatLightPipeline;
    }
}
