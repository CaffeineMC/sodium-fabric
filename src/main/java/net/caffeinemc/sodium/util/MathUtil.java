package net.caffeinemc.sodium.util;

import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Math;
import org.joml.Vector3fc;

public class MathUtil {
    /**
     * @return True if the specified number is greater than zero and is a power of two, otherwise false
     */
    public static boolean isPowerOfTwo(int n) {
        return ((n & (n - 1)) == 0);
    }

    public static long toMib(long x) {
        return x / 1024L / 1024L;
    }

    /**
     * Returns {@param position} aligned to the next multiple of {@param alignment}.
     * @param position The position in bytes
     * @param alignment The alignment in bytes (must be a power-of-two)
     * @return The aligned position, either equal to or greater than {@param position}
     */
    public static int align(int position, int alignment) {
        return ((position - 1) + alignment) & -alignment;
    }

    /**
     * Returns {@param position} aligned to the next multiple of {@param alignment}.
     * @param position The position in bytes
     * @param alignment The alignment in bytes (must be a power-of-two)
     * @return The aligned position, either equal to or greater than {@param position}
     */
    public static long align(long position, long alignment) {
        return ((position - 1) + alignment) & -alignment;
    }

    public static int findNextPositivePowerOfTwo(int value) {
        return 1 << (Integer.SIZE - Integer.numberOfLeadingZeros(value - 1));
    }

    public static long findNextPositivePowerOfTwo(long value) {
        return 1L << (Long.SIZE - Long.numberOfLeadingZeros(value - 1));
    }
    
    /**
     * @author ZNix and burger (originally for BEM)
     * @param mv the model-view matrix
     * @return the camera position of the matrix
     */
    public static Vector3f getCameraPos(Matrix4f mv) {
        // The translation of the inverse of a transform matrix is the negation of
        // the transposed rotation times the transform of the original matrix.
        //
        // The above only works if there's no scaling though - to correct for that, we
        // can find the length of each column is the scaling factor for x, y or z depending
        // on the column number. We then divide each of the output components by the
        // square of the scaling factor - since we're multiplying the scaling factor in a
        // second time with the matrix multiply, we have to divide twice (same as divide by sq root)
        // to get the actual inverse.
    
        // Using fastInverseSqrt might be playing with fire here
//        double undoScaleX = 1.0 / Math.sqrt(mv.m00() * mv.m00() + mv.m10() * mv.m10() + mv.m20() * mv.m20());
//        double undoScaleY = 1.0 / Math.sqrt(mv.m01() * mv.m01() + mv.m11() * mv.m11() + mv.m21() * mv.m21());
//        double undoScaleZ = 1.0 / Math.sqrt(mv.m02() * mv.m02() + mv.m12() * mv.m12() + mv.m22() * mv.m22());
//
//        return new Vector3f(
//                (float) (-(mv.m00() * mv.m03() + mv.m10() * mv.m13() + mv.m20() * mv.m23()) * undoScaleX * undoScaleX),
//                (float) (-(mv.m01() * mv.m03() + mv.m11() * mv.m13() + mv.m21() * mv.m23()) * undoScaleY * undoScaleY),
//                (float) (-(mv.m02() * mv.m03() + mv.m12() * mv.m13() + mv.m22() * mv.m23()) * undoScaleZ * undoScaleZ)
//        );
        
        // Am I crazy or is the sqrt completely useless?
        float scaleXSquared = Math.fma(Math.fma(mv.m00(), mv.m00(), mv.m10()), mv.m10(), mv.m20()) * mv.m20();
        float scaleYSquared = Math.fma(Math.fma(mv.m01(), mv.m01(), mv.m11()), mv.m11(), mv.m21()) * mv.m21();
        float scaleZSquared = Math.fma(Math.fma(mv.m02(), mv.m02(), mv.m12()), mv.m12(), mv.m22()) * mv.m22();
    
        return new Vector3f(
                -(Math.fma(Math.fma(mv.m00(), mv.m03(), mv.m10()), mv.m13(), mv.m20()) * mv.m23()) / scaleXSquared,
                -(Math.fma(Math.fma(mv.m01(), mv.m03(), mv.m11()), mv.m13(), mv.m21()) * mv.m23()) / scaleYSquared,
                -(Math.fma(Math.fma(mv.m02(), mv.m03(), mv.m12()), mv.m13(), mv.m22()) * mv.m23()) / scaleZSquared
        );
    }
    
    /**
     * An adaptation to JOML's {@link Vector3f#angleCos(Vector3fc)} which has primitive inputs for inlining. It also
     * uses fast inverse square root with a multiplication, rather than a square root and a division.
     */
    public static float angleCos(float v1x, float v1y, float v1z, float v2x, float v2y, float v2z) {
        float length1Squared = Math.fma(v1x, v1x, Math.fma(v1y, v1y, v1z * v1z));
        float length2Squared = Math.fma(v2x, v2x, Math.fma(v2y, v2y, v2z * v2z));
        float dot = Math.fma(v1x, v2x, Math.fma(v1y, v2y, v1z * v2z));
        return dot * MathHelper.fastInverseSqrt(length1Squared * length2Squared);
    }
}
