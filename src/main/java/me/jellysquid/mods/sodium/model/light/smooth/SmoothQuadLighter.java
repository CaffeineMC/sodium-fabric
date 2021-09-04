package me.jellysquid.mods.sodium.model.light.smooth;

import me.jellysquid.mods.sodium.interop.fabric.mesh.QuadViewImpl;
import me.jellysquid.mods.sodium.model.light.QuadLighter;
import me.jellysquid.mods.sodium.model.light.data.LightDataAccess;
import me.jellysquid.mods.sodium.model.light.data.QuadLightData;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import me.jellysquid.mods.sodium.interop.fabric.helper.GeometryHelper;
import me.jellysquid.mods.sodium.render.renderer.BlockRenderInfo;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

/**
 * A light pipeline which produces smooth interpolated lighting and ambient occlusion for model quads. This
 * implementation makes a number of improvements over vanilla's own "smooth lighting" option. In no particular order:
 *
 * - Ambient occlusion of block slopes underwater no longer produces broken results (fixes MC-149211)
 * - Smooth lighting now works when underwater (fixes MC-68129)
 * - Corner blocks are now selected from the correct set of neighbors above block faces (fixes MC-148689 and MC-12558)
 * - Shading issues caused by anisotropy are fixed by re-orientating quads to a consistent ordering (fixes MC-136302)
 * - Inset block faces are correctly shaded by their neighbors, fixing a number of problems with non-full blocks such as
 *   grass paths (fixes MC-11783 and MC-108621)
 * - Synchronization issues between the main render thread's light engine and chunk build worker threads are corrected
 *   by copying light data alongside block states, fixing a number of inconsistencies in baked chunks (no open issue)
 *
 * This implementation also includes a significant number of optimizations:
 *
 * - Computed light data for a given block face is cached and re-used again when multiple quads exist for a given
 *   facing, making complex block models less expensive to render
 * - The light data cache encodes as much information as possible into integer words to improve cache locality and
 *   to eliminate the multiple array lookups that would otherwise be needed, significantly speeding up this section
 * - Block faces aligned to the block grid use a fast-path for mapping corner light values to vertices without expensive
 *   interpolation or blending, speeding up most block renders
 * - Some critical code paths have been re-written to hit the JVM's happy path, allowing it to perform auto-vectorization
 *   of the blend functions
 * - Information about a given model quad is cached to enable the light pipeline to make certain assumptions and skip
 *   unnecessary computation
 */
public class SmoothQuadLighter implements QuadLighter {
    private static final int BASIC_QUAD_FLAGS = GeometryHelper.CUBIC_FLAG | GeometryHelper.LIGHT_FACE_FLAG;

    /**
     * The cache which light data will be accessed from.
     */
    private final LightDataAccess lightCache;

    /**
     * The cached face data for each side of a block, both inset and outset.
     */
    private final AoFaceData[] cachedFaceData = new AoFaceData[6 * 2];

    /**
     * The position at which the cached face data was taken at.
     */
    private long cachedPos = Long.MIN_VALUE;

    /**
     * A temporary array for storing the intermediary results of weight data for non-aligned face blending.
     */
    private final float[] weights = new float[4];

    private final QuadLightData lightOutput = new QuadLightData();
    private final BlockRenderInfo blockRenderInfo;

    public SmoothQuadLighter(LightDataAccess cache, BlockRenderInfo blockRenderInfo) {
        this.lightCache = cache;

        for (int i = 0; i < this.cachedFaceData.length; i++) {
            this.cachedFaceData[i] = new AoFaceData();
        }

        this.blockRenderInfo = blockRenderInfo;
    }

    @Override
    public void compute(MutableQuadView quad) {
        this.updateCachedData(this.blockRenderInfo.blockPos.asLong());

        var quadS = (QuadViewImpl) quad;
        int flags = quadS.geometryFlags();

        final AoNeighborInfo neighborInfo = AoNeighborInfo.get(quad.lightFace());
        final QuadLightData out = this.lightOutput;

        // If the model quad is aligned to the block's face and covers it entirely, we can take a fast path and directly
        // map the corner values onto this quad's vertices. This covers most situations during rendering and provides
        // a modest speed-up.
        if ((flags & BASIC_QUAD_FLAGS) == BASIC_QUAD_FLAGS) {
            this.applyBasic(neighborInfo, this.blockRenderInfo.blockPos, quad.lightFace(), out, flags);
        } else {
            this.applyComplex(neighborInfo, quad, this.blockRenderInfo.blockPos, quad.lightFace(), out, flags);
        }

        this.applySidedBrightness(out, quad.lightFace(), quadS.hasShade());
    }

    @Override
    public QuadLightData getQuadLightData() {
        return this.lightOutput;
    }

    private void applySidedBrightness(QuadLightData out, Direction face, boolean shade) {
        float brightness = this.lightCache.getWorld().getBrightness(face, shade);
        float[] br = out.shade;

        for (int i = 0; i < br.length; i++) {
            br[i] *= brightness;
        }
    }

    private void applyComplex(AoNeighborInfo neighborInfo, QuadView quad, BlockPos pos, Direction dir, QuadLightData out, int flags) {
        // TODO: We don't appear to be handling outset faces
        for (int i = 0; i < 4; i++) {
            // Clamp the vertex positions to the block's boundaries to prevent weird errors in lighting
            float cx = clamp(quad.x(i));
            float cy = clamp(quad.y(i));
            float cz = clamp(quad.z(i));

            float[] weights = this.weights;
            neighborInfo.calculateCornerWeights(cx, cy, cz, weights);

            float depth = neighborInfo.getDepth(cx, cy, cz);

            // If the quad is co-planar to the light face, simply interpolate the corner values
            if (MathHelper.approximatelyEquals(depth, 0.0F)) {
                this.applyAlignedPartialFace(pos, dir, weights, i, out, false);
            } else if (MathHelper.approximatelyEquals(depth, 1.0F)) {
                this.applyAlignedPartialFace(pos, dir, weights, i, out, true);
            } else {
                // Blend the occlusion factor between the blocks directly beside this face and the blocks above it
                // based on how inset the face is. This fixes a few issues with blocks such as farmland and paths.
                this.applyInsetPartialFace(pos, dir, depth, 1.0f - depth, weights, i, out);
            }
        }
    }

    private void applyInsetPartialFace(BlockPos pos, Direction dir, float n1d, float n2d, float[] w, int i, QuadLightData out) {
        AoFaceData n1 = this.getCachedFaceData(pos, dir, false);

        if (!n1.hasUnpackedLightData()) {
            n1.unpackLightData();
        }

        AoFaceData n2 = this.getCachedFaceData(pos, dir, true);

        if (!n2.hasUnpackedLightData()) {
            n2.unpackLightData();
        }

        // Blend between the direct neighobors and above based on the passed weights
        float ao = (n1.getBlendedShade(w) * n1d) + (n2.getBlendedShade(w) * n2d);
        float sl = (n1.getBlendedSkyLight(w) * n1d) + (n2.getBlendedSkyLight(w) * n2d);
        float bl = (n1.getBlendedBlockLight(w) * n1d) + (n2.getBlendedBlockLight(w) * n2d);

        out.shade[i] = ao;
        out.texture[i] = getLightMapCoord(sl, bl);
    }

    /**
     * Calculates the light data for a grid-aligned quad that does not cover the entire block volume's face.
     */
    private void applyAlignedPartialFace(BlockPos pos, Direction dir, float[] w, int i, QuadLightData out, boolean offset) {
        AoFaceData faceData = this.getCachedFaceData(pos, dir, offset);

        if (!faceData.hasUnpackedLightData()) {
            faceData.unpackLightData();
        }

        float sl = faceData.getBlendedSkyLight(w);
        float bl = faceData.getBlendedBlockLight(w);
        float ao = faceData.getBlendedShade(w);

        out.shade[i] = ao;
        out.texture[i] = getLightMapCoord(sl, bl);
    }

    /**
     * Quickly calculates the light data for a full grid-aligned quad. This represents the most common case (outward
     * facing quads on a full-block model) and avoids interpolation between neighbors as each corner will only ever
     * have two contributing sides.
     */
    private void applyBasic(AoNeighborInfo neighborInfo, BlockPos pos, Direction dir, QuadLightData out, int flags) {
        AoFaceData faceData = this.getCachedFaceData(pos, dir, (flags & GeometryHelper.LIGHT_FACE_FLAG) != 0);
        neighborInfo.mapCorners(faceData.lm, faceData.ao, out.texture, out.shade);
    }

    /**
     * Returns the cached data for a given facing or calculates it if it hasn't been cached.
     */
    private AoFaceData getCachedFaceData(BlockPos pos, Direction face, boolean offset) {
        AoFaceData data = this.cachedFaceData[offset ? face.ordinal() : face.ordinal() + 6];

        if (!data.hasLightData()) {
            data.initLightData(this.lightCache, pos, face, offset);
        }

        return data;
    }

    private void updateCachedData(long key) {
        if (this.cachedPos != key) {
            for (AoFaceData data : this.cachedFaceData) {
                data.reset();
            }

            this.cachedPos = key;
        }
    }

    /**
     * Clamps the given float to the range [0.0, 1.0].
     */
    private static float clamp(float v) {
        if (v < 0.0f) {
            return 0.0f;
        } else if (v > 1.0f) {
            return 1.0f;
        }

        return v;
    }

    /**
     * Returns a texture coordinate on the light map texture for the given block and sky light values.
     */
    private static int getLightMapCoord(float sl, float bl) {
        return (((int) sl & 0xFF) << 16) | ((int) bl & 0xFF);
    }

}
