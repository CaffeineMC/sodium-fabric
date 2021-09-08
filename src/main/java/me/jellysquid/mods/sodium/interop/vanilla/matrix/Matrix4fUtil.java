package me.jellysquid.mods.sodium.interop.vanilla.matrix;

import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Quaternion;

public class Matrix4fUtil {
    /**
     * Applies the specified rotation to a matrix.
     *
     * @param m The matrix to rotate
     * @param q The quaternion to rotate the matrix by
     */
    public static void rotateMatrix(Matrix4f m, Quaternion q) {
        boolean x = q.getX() != 0.0F;
        boolean y = q.getY() != 0.0F;
        boolean z = q.getZ() != 0.0F;

        // Try to determine if the quaternion is a simple rotation on one axis component only
        if (x) {
            if (!y && !z) {
                rotateMatrixX(m, q);
            } else {
                rotateMatrixXYZ(m, q);
            }
        } else if (y) {
            if (!z) {
                rotateMatrixY(m, q);
            } else {
                rotateMatrixXYZ(m, q);
            }
        } else if (z) {
            rotateMatrixZ(m, q);
        }
    }

    /**
     * Applies the specified translation to the matrix in-place.
     *
     * @param m The matrix to translate
     * @param x The x-component of the translation
     * @param y The y-component of the translation
     * @param z The z-component of the translation
     */
    public static void translateMatrix(Matrix4f m, float x, float y, float z) {
        m.a03 = m.a00 * x + m.a01 * y + m.a02 * z + m.a03;
        m.a13 = m.a10 * x + m.a11 * y + m.a12 * z + m.a13;
        m.a23 = m.a20 * x + m.a21 * y + m.a22 * z + m.a23;
        m.a33 = m.a30 * x + m.a31 * y + m.a32 * z + m.a33;
    }

    /**
     * Applies the matrix transformation to the given input vector, returning the x-component.
     * @param m A 4x4 matrix containing the transformation
     * @param x The x-component of the vector
     * @param y The y-component of the vector
     * @param z The z-component of the vector
     * @return The x-component of the transformed input vector
     */
    public static float transformVectorX(Matrix4f m, float x, float y, float z) {
        return (m.a00 * x) + (m.a01 * y) + (m.a02 * z) + (m.a03 * 1.0f);
    }

    /**
     * Applies the matrix transformation to the given input vector, returning the y-component.
     * @param m A 4x4 matrix containing the transformation
     * @param x The x-component of the vector
     * @param y The y-component of the vector
     * @param z The z-component of the vector
     * @return The y-component of the transformed input vector
     */
    public static float transformVectorY(Matrix4f m, float x, float y, float z) {
        return (m.a10 * x) + (m.a11 * y) + (m.a12 * z) + (m.a13 * 1.0f);
    }

    /**
     * Applies the matrix transformation to the given input vector, returning the z-component.
     * @param m A 4x4 matrix containing the transformation
     * @param x The x-component of the vector
     * @param y The y-component of the vector
     * @param z The z-component of the vector
     * @return The z-component of the transformed input vector
     */
    public static float transformVectorZ(Matrix4f m, float x, float y, float z) {
        return (m.a20 * x) + (m.a21 * y) + (m.a22 * z) + (m.a23 * 1.0f);
    }

    private static void rotateMatrixX(Matrix4f m, Quaternion quaternion) {
        float x = quaternion.getX();
        float w = quaternion.getW();

        float xx = 2.0F * x * x;
        float ta11 = 1.0F - xx;
        float ta22 = 1.0F - xx;

        float xw = x * w;

        float ta21 = 2.0F * xw;
        float ta12 = 2.0F * -xw;

        float a01 = m.a01 * ta11 + m.a02 * ta21;
        float a02 = m.a01 * ta12 + m.a02 * ta22;
        float a11 = m.a11 * ta11 + m.a12 * ta21;
        float a12 = m.a11 * ta12 + m.a12 * ta22;
        float a21 = m.a21 * ta11 + m.a22 * ta21;
        float a22 = m.a21 * ta12 + m.a22 * ta22;
        float a31 = m.a31 * ta11 + m.a32 * ta21;
        float a32 = m.a31 * ta12 + m.a32 * ta22;

        m.a01 = a01;
        m.a02 = a02;
        m.a11 = a11;
        m.a12 = a12;
        m.a21 = a21;
        m.a22 = a22;
        m.a31 = a31;
        m.a32 = a32;
    }

    private static void rotateMatrixY(Matrix4f m, Quaternion quaternion) {
        float y = quaternion.getY();
        float w = quaternion.getW();

        float yy = 2.0F * y * y;
        float ta00 = 1.0F - yy;
        float ta22 = 1.0F - yy;
        float yw = y * w;
        float ta20 = 2.0F * -yw;
        float ta02 = 2.0F * yw;

        float a00 = m.a00 * ta00 + m.a02 * ta20;
        float a02 = m.a00 * ta02 + m.a02 * ta22;
        float a10 = m.a10 * ta00 + m.a12 * ta20;
        float a12 = m.a10 * ta02 + m.a12 * ta22;
        float a20 = m.a20 * ta00 + m.a22 * ta20;
        float a22 = m.a20 * ta02 + m.a22 * ta22;
        float a30 = m.a30 * ta00 + m.a32 * ta20;
        float a32 = m.a30 * ta02 + m.a32 * ta22;

        m.a00 = a00;
        m.a02 = a02;
        m.a10 = a10;
        m.a12 = a12;
        m.a20 = a20;
        m.a22 = a22;
        m.a30 = a30;
        m.a32 = a32;
    }

    private static void rotateMatrixZ(Matrix4f m, Quaternion quaternion) {
        float z = quaternion.getZ();
        float w = quaternion.getW();

        float zz = 2.0F * z * z;
        float ta00 = 1.0F - zz;
        float ta11 = 1.0F - zz;
        float zw = z * w;
        float ta10 = 2.0F * zw;
        float ta01 = 2.0F * -zw;

        float a00 = m.a00 * ta00 + m.a01 * ta10;
        float a01 = m.a00 * ta01 + m.a01 * ta11;
        float a10 = m.a10 * ta00 + m.a11 * ta10;
        float a11 = m.a10 * ta01 + m.a11 * ta11;
        float a20 = m.a20 * ta00 + m.a21 * ta10;
        float a21 = m.a20 * ta01 + m.a21 * ta11;
        float a30 = m.a30 * ta00 + m.a31 * ta10;
        float a31 = m.a30 * ta01 + m.a31 * ta11;

        m.a00 = a00;
        m.a01 = a01;
        m.a10 = a10;
        m.a11 = a11;
        m.a20 = a20;
        m.a21 = a21;
        m.a30 = a30;
        m.a31 = a31;
    }

    private static void rotateMatrixXYZ(Matrix4f m, Quaternion quaternion) {
        float x = quaternion.getX();
        float y = quaternion.getY();
        float z = quaternion.getZ();
        float w = quaternion.getW();

        float xx = 2.0F * x * x;
        float yy = 2.0F * y * y;
        float zz = 2.0F * z * z;
        float ta00 = 1.0F - yy - zz;
        float ta11 = 1.0F - zz - xx;
        float ta22 = 1.0F - xx - yy;
        float xy = x * y;
        float yz = y * z;
        float zx = z * x;
        float xw = x * w;
        float yw = y * w;
        float zw = z * w;
        float ta10 = 2.0F * (xy + zw);
        float ta01 = 2.0F * (xy - zw);
        float ta20 = 2.0F * (zx - yw);
        float ta02 = 2.0F * (zx + yw);
        float ta21 = 2.0F * (yz + xw);
        float ta12 = 2.0F * (yz - xw);

        float a00 = m.a00 * ta00 + m.a01 * ta10 + m.a02 * ta20;
        float a01 = m.a00 * ta01 + m.a01 * ta11 + m.a02 * ta21;
        float a02 = m.a00 * ta02 + m.a01 * ta12 + m.a02 * ta22;
        float a10 = m.a10 * ta00 + m.a11 * ta10 + m.a12 * ta20;
        float a11 = m.a10 * ta01 + m.a11 * ta11 + m.a12 * ta21;
        float a12 = m.a10 * ta02 + m.a11 * ta12 + m.a12 * ta22;
        float a20 = m.a20 * ta00 + m.a21 * ta10 + m.a22 * ta20;
        float a21 = m.a20 * ta01 + m.a21 * ta11 + m.a22 * ta21;
        float a22 = m.a20 * ta02 + m.a21 * ta12 + m.a22 * ta22;
        float a30 = m.a30 * ta00 + m.a31 * ta10 + m.a32 * ta20;
        float a31 = m.a30 * ta01 + m.a31 * ta11 + m.a32 * ta21;
        float a32 = m.a30 * ta02 + m.a31 * ta12 + m.a32 * ta22;

        m.a00 = a00;
        m.a01 = a01;
        m.a02 = a02;
        m.a10 = a10;
        m.a11 = a11;
        m.a12 = a12;
        m.a20 = a20;
        m.a21 = a21;
        m.a22 = a22;
        m.a30 = a30;
        m.a31 = a31;
        m.a32 = a32;
    }
}
