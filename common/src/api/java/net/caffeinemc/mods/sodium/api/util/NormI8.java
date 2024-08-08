package net.caffeinemc.mods.sodium.api.util;

import net.minecraft.util.Mth;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Provides some utilities for working with packed normal vectors. Each normal component provides 8 bits of
 * precision in the range of [-1.0,1.0].
 *
 * | 32        | 24        | 16        | 8          |
 * | 0000 0000 | 0110 1100 | 0110 1100 | 0110 1100  |
 * | Padding   | X         | Y         | Z          |
 */
public class NormI8 {
    private static final int X_COMPONENT_OFFSET = 0;
    private static final int Y_COMPONENT_OFFSET = 8;
    private static final int Z_COMPONENT_OFFSET = 16;

    /**
     * The maximum value of a normal's vector component.
     */
    private static final float COMPONENT_RANGE = 127.0f;

    /**
     * Constant value which can be multiplied with a floating-point vector component to get the normalized value. The
     * multiplication is slightly faster than a floating point division, and this code is a hot path which justifies it.
     */
    private static final float NORM = 1.0f / COMPONENT_RANGE;

    public static int pack(Vector3fc normal) {
        return pack(normal.x(), normal.y(), normal.z());
    }

    /**
     * Packs the specified vector components into a 32-bit integer in XYZ ordering with the 8 bits of padding at the
     * end.
     * @param x The x component of the normal's vector
     * @param y The y component of the normal's vector
     * @param z The z component of the normal's vector
     */
    public static int pack(float x, float y, float z) {
        int normX = encode(x);
        int normY = encode(y);
        int normZ = encode(z);

        return (normZ << Z_COMPONENT_OFFSET) | (normY << Y_COMPONENT_OFFSET) | (normX << X_COMPONENT_OFFSET);
    }

    /**
     * Encodes a float in the range of -1.0..1.0 to a normalized unsigned integer in the range of 0..255 which can then
     * be passed to graphics memory.
     */
    private static int encode(float comp) {
        // TODO: is the clamp necessary here? our inputs should always be normalized vector components
        return ((int) (Mth.clamp(comp, -1.0F, 1.0F) * COMPONENT_RANGE) & 255);
    }

    /**
     * Unpacks the x-component of the packed normal, denormalizing it to a float in the range of -1.0..1.0.
     * @param norm The packed normal
     */
    public static float unpackX(int norm) {
        return ((byte) ((norm >> X_COMPONENT_OFFSET) & 0xFF)) * NORM;
    }

    /**
     * Unpacks the y-component of the packed normal, denormalizing it to a float in the range of -1.0..1.0.
     * @param norm The packed normal
     */
    public static float unpackY(int norm) {
        return ((byte) ((norm >> Y_COMPONENT_OFFSET) & 0xFF)) * NORM;
    }

    /**
     * Unpacks the z-component of the packed normal, denormalizing it to a float in the range of -1.0..1.0.
     * @param norm The packed normal
     */
    public static float unpackZ(int norm) {
        return ((byte) ((norm >> Z_COMPONENT_OFFSET) & 0xFF)) * NORM;
    }

    /**
     * Flips the direction of a packed normal by negating each component. (multiplication by -1)
     * @param norm The packed normal
     */
    public static int flipPacked(int norm) {
        int normX = (((norm >> X_COMPONENT_OFFSET) & 0xFF) * -1) & 0xFF;
        int normY = (((norm >> Y_COMPONENT_OFFSET) & 0xFF) * -1) & 0xFF;
        int normZ = (((norm >> Z_COMPONENT_OFFSET) & 0xFF) * -1) & 0xFF;

        return (normZ << Z_COMPONENT_OFFSET) | (normY << Y_COMPONENT_OFFSET) | (normX << X_COMPONENT_OFFSET);
    }

    /**
     * Returns true if the two packed normals are opposite directions.
     *
     * TODO: this could possibly be faster by using normA == (~normB + 0x010101) but
     * that has to special case when a component is zero since that wouldn't
     * overflow correctly back to zero. (~0+1 == 0 but not if it's somewhere inside
     * th int)
     *
     * @param normA The first packed normal
     * @param normB The second packed normal
     */
    public static boolean isOpposite(int normA, int normB) {
        // use byte to automatically sign extend the components
        byte normAX = (byte) (normA >> X_COMPONENT_OFFSET);
        byte normAY = (byte) (normA >> Y_COMPONENT_OFFSET);
        byte normAZ = (byte) (normA >> Z_COMPONENT_OFFSET);

        byte normBX = (byte) (normB >> X_COMPONENT_OFFSET);
        byte normBY = (byte) (normB >> Y_COMPONENT_OFFSET);
        byte normBZ = (byte) (normB >> Z_COMPONENT_OFFSET);

        return normAX == -normBX && normAY == -normBY && normAZ == -normBZ;
    }

    public static Vector3f unpack(int packed, Vector3f output) {
        return output.set(unpackX(packed), unpackY(packed), unpackZ(packed));
    }
}
