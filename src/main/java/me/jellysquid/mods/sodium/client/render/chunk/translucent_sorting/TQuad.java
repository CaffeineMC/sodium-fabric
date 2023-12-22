package me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting;

import java.util.Arrays;

import org.joml.Vector3f;
import org.joml.Vector3fc;

import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.api.util.NormI8;

/**
 * Represents a quad for the purposes of translucency sorting. Called TQuad to
 * avoid confusion with other quad classes.
 */
public class TQuad {
    /**
     * The quantization factor with which the normals are quantized such that there
     * are fewer possible unique normals. The factor describes the number of steps
     * in each direction per dimension that the components of the normals can have.
     * It determines the density of the grid on the surface of a unit cube centered
     * at the origin onto which the normals are projected. The normals are snapped
     * to the nearest grid point.
     */
    private static final int QUANTIZATION_FACTOR = 4;

    private ModelQuadFacing facing;
    private float[] extents;
    private Vector3fc center; // null on aligned quads
    private int packedNormal;
    private Vector3fc quantizedNormal;
    private float dotProduct;

    private TQuad(ModelQuadFacing facing, float[] extents, Vector3fc center, int packedNormal) {
        this.facing = facing;
        this.extents = extents;
        this.center = center;
        this.packedNormal = packedNormal;

        if (this.facing.isAligned()) {
            this.dotProduct = this.extents[this.facing.ordinal()] * this.facing.getSign();
        } else {
            float normX = NormI8.unpackX(this.packedNormal);
            float normY = NormI8.unpackY(this.packedNormal);
            float normZ = NormI8.unpackZ(this.packedNormal);
            this.dotProduct = this.getCenter().dot(normX, normY, normZ);
        }
    }

    static TQuad fromAligned(ModelQuadFacing facing, float[] extents) {
        return new TQuad(facing, extents, null, ModelQuadFacing.PACKED_ALIGNED_NORMALS[facing.ordinal()]);
    }

    static TQuad fromUnaligned(ModelQuadFacing facing, float[] extents, Vector3fc center, int packedNormal) {
        return new TQuad(facing, extents, center, packedNormal);
    }

    public ModelQuadFacing getFacing() {
        return this.facing;
    }

    public float[] getExtents() {
        return this.extents;
    }

    public Vector3fc getCenter() {
        // calculate aligned quad center on demand
        if (this.center == null) {
            this.center = new Vector3f(
                    (this.extents[0] + this.extents[3]) / 2,
                    (this.extents[1] + this.extents[4]) / 2,
                    (this.extents[2] + this.extents[5]) / 2);
        }
        return this.center;
    }

    public float getDotProduct() {
        return this.dotProduct;
    }

    public int getPackedNormal() {
        return this.packedNormal;
    }

    public static boolean isOpposite(int normA, int normB) {
        return NormI8.isOpposite(normA, normB);
    }

    public Vector3fc getQuantizedNormal() {
        if (this.quantizedNormal == null) {
            if (this.facing.isAligned()) {
                this.quantizedNormal = this.facing.getAlignedNormal();
            } else {
                this.computeQuantizedNormal();
            }
        }
        return this.quantizedNormal;
    }

    private void computeQuantizedNormal() {
        float normX = NormI8.unpackX(this.packedNormal);
        float normY = NormI8.unpackY(this.packedNormal);
        float normZ = NormI8.unpackZ(this.packedNormal);

        // normalize onto the surface of a cube by dividing by the length of the longest
        // component
        float infNormLength = Math.max(Math.abs(normX), Math.max(Math.abs(normY), Math.abs(normZ)));
        if (infNormLength != 0 && infNormLength != 1) {
            normX /= infNormLength;
            normY /= infNormLength;
            normZ /= infNormLength;
        }

        // quantize the coordinates on the surface of the cube.
        // in each axis the number of values is 2 * QUANTIZATION_FACTOR + 1.
        // the total number of normals is the number of points on that cube's surface.
        var normal = new Vector3f(
                (int) (normX * QUANTIZATION_FACTOR),
                (int) (normY * QUANTIZATION_FACTOR),
                (int) (normZ * QUANTIZATION_FACTOR));
        normal.normalize();
        this.quantizedNormal = normal;
    }

    int getQuadHash() {
        // the hash code needs to be particularly collision resistant
        int result = 1;
        result = 31 * result + Arrays.hashCode(this.extents);
        if (facing.isAligned()) {
            result = 31 * result + this.facing.hashCode();
        } else {
            result = 31 * result + this.packedNormal;
        }
        result = 31 * result + Float.hashCode(this.dotProduct);
        return result;
    }

    public float getAlignedSurfaceArea() {
        if (!this.facing.isAligned()) {
            return 100;
        }

        var dX = this.extents[3] - this.extents[0];
        var dY = this.extents[4] - this.extents[1];
        var dZ = this.extents[5] - this.extents[2];

        if (dX == 0) {
            return (float) (dY * dZ);
        } else if (dY == 0) {
            return (float) (dX * dZ);
        } else if (dZ == 0) {
            return (float) (dX * dY);
        } else {
            // non-flat aligned quad, weird edge case
            return 90;
        }
    }

    public boolean extentsEqual(float[] other) {
        return extentsEqual(this.extents, other);
    }

    public static boolean extentsEqual(float[] a, float[] b) {
        for (int i = 0; i < 6; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }
}
