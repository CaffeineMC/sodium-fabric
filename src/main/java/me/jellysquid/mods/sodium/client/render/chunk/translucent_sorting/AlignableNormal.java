package me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting;

import org.joml.Vector3f;
import org.joml.Vector3fc;

import it.unimi.dsi.fastutil.floats.FloatArrays;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;

/**
 * A normal vector that has additional information about its alignment. This is
 * useful for better hashing and telling other code that the normal is aligned,
 * which in turn enables many optimizations and fast paths to be taken.
 */
public class AlignableNormal extends Vector3f {
    private static final AlignableNormal[] NORMALS = new AlignableNormal[ModelQuadFacing.DIRECTIONS];

    static {
        for (int i = 0; i < ModelQuadFacing.DIRECTIONS; i++) {
            NORMALS[i] = new AlignableNormal(ModelQuadFacing.ALIGNED_NORMALS[i], i);
        }
    }

    private static final int UNASSIGNED = ModelQuadFacing.UNASSIGNED.ordinal();
    private final int alignedDirection;

    private AlignableNormal(Vector3fc v, int alignedDirection) {
        super(v);
        this.alignedDirection = alignedDirection;
    }

    public static AlignableNormal fromAligned(int alignedDirection) {
        return NORMALS[alignedDirection];
    }

    public static AlignableNormal fromUnaligned(Vector3fc v) {
        return new AlignableNormal(v, UNASSIGNED);
    }

    public int getAlignedDirection() {
        return this.alignedDirection;
    }

    public boolean isAligned() {
        return this.alignedDirection != UNASSIGNED;
    }

    @Override
    public int hashCode() {
        if (this.isAligned()) {
            return this.alignedDirection;
        } else {
            return super.hashCode() + ModelQuadFacing.DIRECTIONS;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        AlignableNormal other = (AlignableNormal) obj;
        if (alignedDirection != other.alignedDirection)
            return false;
        return true;
    }

    public static boolean queryRange(float[] sortedDistances, float start, float end) {
        // test that there is actually an entry in the query range
        int result = FloatArrays.binarySearch(sortedDistances, start);
        if (result < 0) {
            // recover the insertion point
            int insertionPoint = -result - 1;
            if (insertionPoint >= sortedDistances.length) {
                // no entry in the query range
                return false;
            }

            // check if the entry at the insertion point, which is the next one greater than
            // the start value, is less than or equal to the end value
            if (sortedDistances[insertionPoint] <= end) {
                // there is an entry in the query range
                return true;
            }
        } else {
            // exact match, trigger
            return true;
        }
        return false;
    }
}
