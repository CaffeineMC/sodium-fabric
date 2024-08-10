package net.caffeinemc.mods.sodium.client.model.light.smooth;

import net.caffeinemc.mods.sodium.client.model.light.data.LightDataAccess;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import static net.caffeinemc.mods.sodium.client.model.light.data.ArrayLightDataCache.*;

class AoFaceData {
    public final int[] lm = new int[4];

    public final float[] ao = new float[4];
    public final float[] bl = new float[4];
    public final float[] sl = new float[4];

    private int flags;

    public void initLightData(LightDataAccess cache, BlockPos pos, Direction direction, boolean offset) {
        final int x = pos.getX();
        final int y = pos.getY();
        final int z = pos.getZ();

        final int adjX;
        final int adjY;
        final int adjZ;

        if (offset) {
            adjX = x + direction.getStepX();
            adjY = y + direction.getStepY();
            adjZ = z + direction.getStepZ();
        } else {
            adjX = x;
            adjY = y;
            adjZ = z;
        }

        final int adjWord = cache.get(adjX, adjY, adjZ);

        final int calm;
        final boolean caem;

        if (offset && unpackFO(adjWord)) {
            final int originWord = cache.get(x, y, z);
            calm = getLightmap(originWord);
            caem = unpackEM(originWord);
        } else {
            calm = getLightmap(adjWord);
            caem = unpackEM(adjWord);
        }

        final float caao = unpackAO(adjWord);

        Direction[] faces = AoNeighborInfo.get(direction).faces;

        final int e0 = cache.get(adjX, adjY, adjZ, faces[0]);
        final int e0lm = getLightmap(e0);
        final float e0ao = unpackAO(e0);
        final boolean e0op = unpackOP(e0);
        final boolean e0em = unpackEM(e0);

        final int e1 = cache.get(adjX, adjY, adjZ, faces[1]);
        final int e1lm = getLightmap(e1);
        final float e1ao = unpackAO(e1);
        final boolean e1op = unpackOP(e1);
        final boolean e1em = unpackEM(e1);

        final int e2 = cache.get(adjX, adjY, adjZ, faces[2]);
        final int e2lm = getLightmap(e2);
        final float e2ao = unpackAO(e2);
        final boolean e2op = unpackOP(e2);
        final boolean e2em = unpackEM(e2);

        final int e3 = cache.get(adjX, adjY, adjZ, faces[3]);
        final int e3lm = getLightmap(e3);
        final float e3ao = unpackAO(e3);
        final boolean e3op = unpackOP(e3);
        final boolean e3em = unpackEM(e3);

        // If neither edge of a corner is occluded, then use the light
        final int c0lm;
        final float c0ao;
        final boolean c0em;

        if (e2op && e0op) {
            c0lm = e0lm;
            c0ao = e0ao;
            c0em = e0em;
        } else {
            int d0 = cache.get(adjX, adjY, adjZ, faces[0], faces[2]);
            c0lm = getLightmap(d0);
            c0ao = unpackAO(d0);
            c0em = unpackEM(d0);
        }

        final int c1lm;
        final float c1ao;
        final boolean c1em;

        if (e3op && e0op) {
            c1lm = e0lm;
            c1ao = e0ao;
            c1em = e0em;
        } else {
            int d1 = cache.get(adjX, adjY, adjZ, faces[0], faces[3]);
            c1lm = getLightmap(d1);
            c1ao = unpackAO(d1);
            c1em = unpackEM(d1);
        }

        final int c2lm;
        final float c2ao;
        final boolean c2em;

        if (e2op && e1op) {
            // FIX: Use e1 instead of e0 to fix lighting errors in some directions
            c2lm = e1lm;
            c2ao = e1ao;
            c2em = e1em;
        } else {
            int d2 = cache.get(adjX, adjY, adjZ, faces[1], faces[2]);
            c2lm = getLightmap(d2);
            c2ao = unpackAO(d2);
            c2em = unpackEM(d2);
        }

        final int c3lm;
        final float c3ao;
        final boolean c3em;

        if (e3op && e1op) {
            // FIX: Use e1 instead of e0 to fix lighting errors in some directions
            c3lm = e1lm;
            c3ao = e1ao;
            c3em = e1em;
        } else {
            int d3 = cache.get(adjX, adjY, adjZ, faces[1], faces[3]);
            c3lm = getLightmap(d3);
            c3ao = unpackAO(d3);
            c3em = unpackEM(d3);
        }

        float[] ao = this.ao;
        ao[0] = (e3ao + e0ao + c1ao + caao) * 0.25f;
        ao[1] = (e2ao + e0ao + c0ao + caao) * 0.25f;
        ao[2] = (e2ao + e1ao + c2ao + caao) * 0.25f;
        ao[3] = (e3ao + e1ao + c3ao + caao) * 0.25f;

        int[] cb = this.lm;
        cb[0] = calculateCornerBrightness(e3lm, e0lm, c1lm, calm, e3em, e0em, c1em, caem);
        cb[1] = calculateCornerBrightness(e2lm, e0lm, c0lm, calm, e2em, e0em, c0em, caem);
        cb[2] = calculateCornerBrightness(e2lm, e1lm, c2lm, calm, e2em, e1em, c2em, caem);
        cb[3] = calculateCornerBrightness(e3lm, e1lm, c3lm, calm, e3em, e1em, c3em, caem);

        this.flags |= AoCompletionFlags.HAS_LIGHT_DATA;
    }

    static AoFaceData weightedMean(AoFaceData in0, float w0, AoFaceData in1, float w1, AoFaceData out) {
        out.ao[0] = in0.ao[0] * w0 + in1.ao[0] * w1;
        out.ao[1] = in0.ao[1] * w0 + in1.ao[1] * w1;
        out.ao[2] = in0.ao[2] * w0 + in1.ao[2] * w1;
        out.ao[3] = in0.ao[3] * w0 + in1.ao[3] * w1;

        if (!in0.hasUnpackedLightData()) in0.unpackLightData();
        if (!in1.hasUnpackedLightData()) in1.unpackLightData();

        out.bl[0] = (int) (in0.bl[0] * w0 + in1.bl[0] * w1);
        out.bl[1] = (int) (in0.bl[1] * w0 + in1.bl[1] * w1);
        out.bl[2] = (int) (in0.bl[2] * w0 + in1.bl[2] * w1);
        out.bl[3] = (int) (in0.bl[3] * w0 + in1.bl[3] * w1);

        out.sl[0] = (int) (in0.sl[0] * w0 + in1.sl[0] * w1);
        out.sl[1] = (int) (in0.sl[1] * w0 + in1.sl[1] * w1);
        out.sl[2] = (int) (in0.sl[2] * w0 + in1.sl[2] * w1);
        out.sl[3] = (int) (in0.sl[3] * w0 + in1.sl[3] * w1);

        out.lm[0] = packLight(out.sl[0], out.bl[0]);
        out.lm[1] = packLight(out.sl[1], out.bl[1]);
        out.lm[2] = packLight(out.sl[2], out.bl[2]);
        out.lm[3] = packLight(out.sl[3], out.bl[3]);

        return out;
    }

    public void unpackLightData() {
        int[] lm = this.lm;

        float[] bl = this.bl;
        float[] sl = this.sl;

        bl[0] = unpackBlockLight(lm[0]);
        bl[1] = unpackBlockLight(lm[1]);
        bl[2] = unpackBlockLight(lm[2]);
        bl[3] = unpackBlockLight(lm[3]);

        sl[0] = unpackSkyLight(lm[0]);
        sl[1] = unpackSkyLight(lm[1]);
        sl[2] = unpackSkyLight(lm[2]);
        sl[3] = unpackSkyLight(lm[3]);

        this.flags |= AoCompletionFlags.HAS_UNPACKED_LIGHT_DATA;
    }

    public float getBlendedSkyLight(float[] w) {
        return weightedSum(this.sl, w);
    }

    public float getBlendedBlockLight(float[] w) {
        return weightedSum(this.bl, w);
    }

    public float getBlendedShade(float[] w) {
        return weightedSum(this.ao, w);
    }

    private static float weightedSum(float[] v, float[] w) {
        float t0 = v[0] * w[0];
        float t1 = v[1] * w[1];
        float t2 = v[2] * w[2];
        float t3 = v[3] * w[3];

        return t0 + t1 + t2 + t3;
    }

    private static float unpackSkyLight(int i) {
        return (i >> 16) & 0xFF;
    }

    private static float unpackBlockLight(int i) {
        return i & 0xFF;
    }

    private static int packLight(float sl, float bl) {
        return (((int) sl & 0xFF) << 16) | ((int) bl & 0xFF);
    }

    private static int calculateCornerBrightness(int a, int b, int c, int d, boolean aem, boolean bem, boolean cem, boolean dem) {
        // FIX: Normalize corner vectors correctly to the minimum non-zero value between each one to prevent
        // strange issues
        if ((a == 0) || (b == 0) || (c == 0) || (d == 0)) {
            // Find the minimum value between all corners
            final int min = minNonZero(minNonZero(a, b), minNonZero(c, d));

            // Normalize the corner values
            a = Math.max(a, min);
            b = Math.max(b, min);
            c = Math.max(c, min);
            d = Math.max(d, min);
        }

        // FIX: Apply the fullbright lightmap from emissive blocks at the very end so it cannot influence
        // the minimum lightmap and produce incorrect results (for example, sculk sensors in a dark room)
        if (aem) {
            a = LightTexture.FULL_BRIGHT;
        }
        if (bem) {
            b = LightTexture.FULL_BRIGHT;
        }
        if (cem) {
            c = LightTexture.FULL_BRIGHT;
        }
        if (dem) {
            d = LightTexture.FULL_BRIGHT;
        }

        return ((a + b + c + d) >> 2) & 0xFF00FF;
    }

    private static int minNonZero(int a, int b) {
        if (a == 0) {
            return b;
        } else if (b == 0) {
            return a;
        }

        return Math.min(a, b);
    }

    public boolean hasLightData() {
        return (this.flags & AoCompletionFlags.HAS_LIGHT_DATA) != 0;
    }

    public boolean hasUnpackedLightData() {
        return (this.flags & AoCompletionFlags.HAS_UNPACKED_LIGHT_DATA) != 0;
    }

    public void reset() {
        this.flags = 0;
    }
}
