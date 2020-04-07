package me.jellysquid.mods.sodium.client.render.light.smooth;

import me.jellysquid.mods.sodium.client.render.light.LightPipeline;
import me.jellysquid.mods.sodium.client.render.light.LightResult;
import me.jellysquid.mods.sodium.client.render.light.cache.LightDataCache;
import me.jellysquid.mods.sodium.client.render.quad.ModelQuadFlags;
import me.jellysquid.mods.sodium.client.render.quad.ModelQuadView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

public class SmoothLightPipeline implements LightPipeline {
    private final LightDataCache lightCache;

    private final AoFaceData[] cachedFaceData = new AoFaceData[6 * 2];

    private final float[] weights = new float[4];

    public SmoothLightPipeline(LightDataCache lightCache) {
        this.lightCache = lightCache;

        for (int i = 0; i < this.cachedFaceData.length; i++) {
            this.cachedFaceData[i] = new AoFaceData();
        }
    }

    @Override
    public void reset() {
        for (AoFaceData data : this.cachedFaceData) {
            data.reset();
        }
    }

    @Override
    public void apply(ModelQuadView quad, BlockPos pos, LightResult out) {
        Direction face = quad.getFacing();
        int flags = quad.getFlags();

        final AoNeighborInfo neighborInfo = AoNeighborInfo.get(face);

        if (!ModelQuadFlags.contains(flags, ModelQuadFlags.IS_ALIGNED) || ModelQuadFlags.contains(flags, ModelQuadFlags.IS_PARTIAL)) {
            this.applyComplex(neighborInfo, quad, pos, face, out);
        } else {
            this.applyAlignedFullFace(neighborInfo, pos, face, out);
        }
    }

    private void applyComplex(AoNeighborInfo neighborInfo, ModelQuadView quad, BlockPos pos, Direction dir, LightResult out) {
        for (int i = 0; i < 4; i++) {
            float cx = clamp(quad.getX(i));
            float cy = clamp(quad.getY(i));
            float cz = clamp(quad.getZ(i));

            float[] weights = this.weights;

            float depth = neighborInfo.getDepth(cx, cy, cz);
            neighborInfo.calculateCornerWeights(cx, cy, cz, weights);

            if (MathHelper.approximatelyEquals(depth, 0.0F)) {
                this.applyAlignedPartialFace(pos, dir, weights, i, out, true);
            } else if (MathHelper.approximatelyEquals(depth, 1.0F)) {
                this.applyAlignedPartialFace(pos, dir, weights, i, out, false);
            } else {
                this.applyInsetPartialFace(pos, dir, depth, 1.0f - depth, weights, i, out);
            }
        }
    }

    private void applyInsetPartialFace(BlockPos pos, Direction dir, float n1d, float n2d, float[] w, int i, LightResult out) {
        AoFaceData n1 = this.getCachedFaceData(pos, dir, false);

        if (!n1.hasUnpackedLightData()) {
            n1.unpackLightData();
        }

        AoFaceData n2 = this.getCachedFaceData(pos, dir, true);

        if (!n2.hasUnpackedLightData()) {
            n2.unpackLightData();
        }

        float ao = (n1.getBlendedShade(w) * n1d) + (n2.getBlendedShade(w) * n2d);
        float sl = (n1.getBlendedSkyLight(w) * n1d) + (n2.getBlendedSkyLight(w) * n2d);
        float bl = (n1.getBlendedBlockLight(w) * n1d) + (n2.getBlendedBlockLight(w) * n2d);

        out.br[i] = ao;
        out.lm[i] = getLightMapCoord(sl, bl);
    }

    private void applyAlignedPartialFace(BlockPos pos, Direction dir, float[] w, int i, LightResult out, boolean offset) {
        AoFaceData faceData = this.getCachedFaceData(pos, dir, offset);

        if (!faceData.hasUnpackedLightData()) {
            faceData.unpackLightData();
        }

        float sl = faceData.getBlendedSkyLight(w);
        float bl = faceData.getBlendedBlockLight(w);
        float ao = faceData.getBlendedShade(w);

        out.br[i] = ao;
        out.lm[i] = getLightMapCoord(sl, bl);
    }

    private void applyAlignedFullFace(AoNeighborInfo neighborInfo, BlockPos pos, Direction dir, LightResult out) {
        AoFaceData faceData = this.getCachedFaceData(pos, dir, true);
        neighborInfo.mapCorners(faceData.lm, faceData.ao, out.lm, out.br);
    }

    private AoFaceData getCachedFaceData(BlockPos pos, Direction face, boolean offset) {
        AoFaceData data = this.cachedFaceData[face.ordinal() + (offset ? 6 : 0)];

        if (!data.hasLightData()) {
            data.initLightData(this.lightCache, pos, face, offset);
        }

        return data;
    }

    private static float clamp(float v) {
        if (v < 0.0f) {
            return 0.0f;
        } else if (v > 1.0f) {
            return 1.0f;
        }

        return v;
    }

    private static int getLightMapCoord(float sl, float bl) {
        return (toTexCoord(sl) << 16) | toTexCoord(bl);
    }

    private static int toTexCoord(float value) {
        return (int) value & 0xFF;
    }
}
