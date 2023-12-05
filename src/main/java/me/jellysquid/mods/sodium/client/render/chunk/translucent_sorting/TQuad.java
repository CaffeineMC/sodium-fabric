package me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting;

import java.util.Arrays;

import org.joml.Vector3f;
import org.joml.Vector3fc;

import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;

/**
 * Represents a quad for the purposes of translucency sorting. Called TQuad to
 * avoid confusion with other quad classes.
 */
public record TQuad(ModelQuadFacing facing, Vector3fc normal, Vector3f center, float[] extents) {
    int getQuadHash() {
        // the hash code needs to be particularly collision resistant
        int result = 1;
        result = 31 * result + Arrays.hashCode(this.extents);
        if (facing == ModelQuadFacing.UNASSIGNED) {
            result = 31 * result + this.normal.hashCode();
        } else {
            result = 31 * result + this.facing.hashCode();
        }
        result = 31 * result + this.center.hashCode();
        return result;
    }

    public float getAlignedSurfaceArea() {
        if (this.facing == ModelQuadFacing.UNASSIGNED) {
            return 0;
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
            return 0;
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
