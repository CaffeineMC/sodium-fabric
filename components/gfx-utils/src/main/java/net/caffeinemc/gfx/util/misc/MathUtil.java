package net.caffeinemc.gfx.util.misc;

import net.caffeinemc.gfx.api.device.RenderConfiguration;
import org.apache.commons.lang3.Validate;
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
    
    public static boolean isPowerOfTwo(long n) {
        return ((n & (n - 1)) == 0);
    }
    
    public static long toMib(long x) {
        return x / 1024L / 1024L;
    }
    
    /**
     * Returns {@param num} aligned to the next multiple of {@param alignment}.
     * @param num The number that will be rounded if needed
     * @param alignment The multiple that the output will be rounded to (must be a power-of-two)
     * @return The aligned position, either equal to or greater than {@param num}
     */
    public static int align(int num, int alignment) {
        if (RenderConfiguration.DEBUG_CHECKS) {
            Validate.isTrue(isPowerOfTwo(alignment), "alignment needs to be a power of two");
        }
        int additive = alignment - 1;
        int mask = ~additive;
        return (num + additive) & mask;
    }
    
    /**
     * Returns {@param num} aligned to the next multiple of {@param alignment}.
     * @param num The number that will be rounded if needed
     * @param alignment The multiple that the output will be rounded to (must be a power-of-two)
     * @return The aligned position, either equal to or greater than {@param num}
     */
    public static long align(long num, long alignment) {
        if (RenderConfiguration.DEBUG_CHECKS) {
            Validate.isTrue(isPowerOfTwo(alignment), "alignment needs to be a power of two");
        }
        long additive = alignment - 1;
        long mask = ~additive;
        return (num + additive) & mask;
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
        return dot * fastInverseSqrt(length1Squared * length2Squared);
    }
    
    // source: https://www.researchgate.net/publication/349378473_Modified_Fast_Inverse_Square_Root_and_Square_Root_Approximation_Algorithms_The_Method_of_Switching_Magic_Constants
//    public static float fastInverseSqrt(float x) {
//        int i = Float.floatToIntBits(x);
//        int k = i & 0x00800000;
//
//        int magic1;
//        float magic2;
//        float magic3;
//        if (k != 0) {
//            magic1 = 0x5ed9e91f;
//            magic2 = 2.33124256f;
//            magic3 = 1.0749737f;
//        } else {
//            magic1 = 0x5f19e8fc;
//            magic2 = 0.824218631f;
//            magic3 = 2.1499474f;
//        }
//
//        i = magic1 - (i >> 1);
//        float y = Float.intBitsToFloat(i);
//        return magic2 * y * Math.fma(-x, y * y, magic3);
//    }
    
    // source: https://www.researchgate.net/publication/349378473_Modified_Fast_Inverse_Square_Root_and_Square_Root_Approximation_Algorithms_The_Method_of_Switching_Magic_Constants
    private static final int CHECKED_BIT = 23;
    private static final int CHECKED_BIT_MASK = 1 << CHECKED_BIT;
    private static final int MAGIC_CONSTANT_COUNT = 3;
    private static final int[] MAGIC_CONSTANTS = {
            0x5ed9e91f,
            Float.floatToRawIntBits(2.33124256f),
            Float.floatToRawIntBits(1.0749737f),
            0x5f19e8fc,
            Float.floatToRawIntBits(0.824218631f),
            Float.floatToRawIntBits(2.1499474f)
    };
    
    public static float fastInverseSqrt(float x) {
        int i = Float.floatToRawIntBits(x);
    
        int magicOffset = ((i & CHECKED_BIT_MASK) >> CHECKED_BIT) * MAGIC_CONSTANT_COUNT;
        int magic1 = MAGIC_CONSTANTS[magicOffset];
        float magic2 = Float.intBitsToFloat(MAGIC_CONSTANTS[magicOffset + 1]);
        float magic3 = Float.intBitsToFloat(MAGIC_CONSTANTS[magicOffset + 2]);
        
        i = magic1 - (i >> 1);
        float y = Float.intBitsToFloat(i);
        return magic2 * y * Math.fma(-x, y * y, magic3);
    }
    
    public static int absMin(int a, int b) {
        return java.lang.Math.abs(a) < java.lang.Math.abs(b) ? a : b;
    }
    
    public static int absMax(int a, int b) {
        return java.lang.Math.abs(a) > java.lang.Math.abs(b) ? a : b;
    }
    
    public static int ceilDiv(int x, int y) {
        int r = x / y;
        // if the signs are the same and modulo not zero, round up
        if ((x ^ y) >= 0 && (r * y != x)) {
            r++;
        }
        return r;
    }
}
