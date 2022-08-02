package net.caffeinemc.sodium.interop.vanilla.math.matrix;

import net.caffeinemc.sodium.util.packed.Normal3b;
import net.minecraft.util.math.*;

public class Matrix3fUtil {
    /**
     * Applies the specified rotation to a matrix in-place.
     *
     * @param m the matrix to transform
     * @param quaternion The quaternion to rotate the matrix by
     */
    public static void rotateMatrix(Matrix3f m, Quaternion quaternion) {
        boolean x = quaternion.getX() != 0.0F;
        boolean y = quaternion.getY() != 0.0F;
        boolean z = quaternion.getZ() != 0.0F;

        // Try to determine if m is a simple rotation on one axis component only
        if (x) {
            if (!y && !z) {
                rotateMatrixX(m, quaternion);
            } else {
                rotateMatrixXYZ(m, quaternion);
            }
        } else if (y) {
            if (!z) {
                rotateMatrixY(m, quaternion);
            } else {
                rotateMatrixXYZ(m, quaternion);
            }
        } else if (z) {
            rotateMatrixZ(m, quaternion);
        }
    }

    public static int transformNormal(Matrix3f m, float x, float y, float z) {
        return Normal3b.pack(
                transformVectorX(m, x, y, z),
                transformVectorY(m, x, y, z),
                transformVectorZ(m, x, y, z)
        );
    }

    public static int transformNormal(Matrix3f m, Direction dir) {
        Vec3i vector = dir.getVector();

        float x = vector.getX();
        float y = vector.getY();
        float z = vector.getZ();

        return transformNormal(m, x, y, z);
    }

    public static int transformNormal(Matrix3f m, Vec3f vector) {
        float x = vector.getX();
        float y = vector.getY();
        float z = vector.getZ();

        return transformNormal(m, x, y, z);
    }

    public static float transformVectorX(Matrix3f m, float x, float y, float z) {
        return m.a00 * x + m.a01 * y + m.a02 * z;
    }

    public static float transformVectorY(Matrix3f m, float x, float y, float z) {
        return m.a10 * x + m.a11 * y + m.a12 * z;
    }

    public static float transformVectorZ(Matrix3f m, float x, float y, float z) {
        return m.a20 * x + m.a21 * y + m.a22 * z;
    }

    public static float transformVectorX(Matrix3f m, Vec3f v) {
        return transformVectorX(m, v.getX(), v.getY(), v.getZ());
    }

    public static float transformVectorY(Matrix3f m, Vec3f v) {
        return transformVectorY(m, v.getX(), v.getY(), v.getZ());
    }

    public static float transformVectorZ(Matrix3f m, Vec3f v) {
        return transformVectorZ(m, v.getX(), v.getY(), v.getZ());
    }

    private static void rotateMatrixX(Matrix3f m, Quaternion quaternion) {
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

        m.a01 = a01;
        m.a02 = a02;
        m.a11 = a11;
        m.a12 = a12;
        m.a21 = a21;
        m.a22 = a22;
    }

    private static void rotateMatrixY(Matrix3f m, Quaternion quaternion) {
        float y = quaternion.getY();
        float w = quaternion.getW();

        float yy = 2.0F * y * y;

        float ta00 = 1.0F - yy;
        float ta22 = 1.0F - yy;

        float yw = y * w;

        float ta20 = 2.0F * (-yw);
        float ta02 = 2.0F * (+yw);

        float a00 = m.a00 * ta00 + m.a02 * ta20;
        float a02 = m.a00 * ta02 + m.a02 * ta22;
        float a10 = m.a10 * ta00 + m.a12 * ta20;
        float a12 = m.a10 * ta02 + m.a12 * ta22;
        float a20 = m.a20 * ta00 + m.a22 * ta20;
        float a22 = m.a20 * ta02 + m.a22 * ta22;

        m.a00 = a00;
        m.a02 = a02;
        m.a10 = a10;
        m.a12 = a12;
        m.a20 = a20;
        m.a22 = a22;
    }

    private static void rotateMatrixZ(Matrix3f m, Quaternion quaternion) {
        float z = quaternion.getZ();
        float w = quaternion.getW();

        float zz = 2.0F * z * z;

        float ta00 = 1.0F - zz;
        float ta11 = 1.0F - zz;

        float zw = z * w;

        float ta10 = 2.0F * (0.0F + zw);
        float ta01 = 2.0F * (0.0F - zw);

        float a00 = m.a00 * ta00 + m.a01 * ta10;
        float a01 = m.a00 * ta01 + m.a01 * ta11;
        float a10 = m.a10 * ta00 + m.a11 * ta10;
        float a11 = m.a10 * ta01 + m.a11 * ta11;
        float a20 = m.a20 * ta00 + m.a21 * ta10;
        float a21 = m.a20 * ta01 + m.a21 * ta11;

        m.a00 = a00;
        m.a01 = a01;
        m.a10 = a10;
        m.a11 = a11;
        m.a20 = a20;
        m.a21 = a21;
    }

    private static void rotateMatrixXYZ(Matrix3f m, Quaternion quaternion) {
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

        m.a00 = a00;
        m.a01 = a01;
        m.a02 = a02;
        m.a10 = a10;
        m.a11 = a11;
        m.a12 = a12;
        m.a20 = a20;
        m.a21 = a21;
        m.a22 = a22;
    }

    public static int transformPackedNormal(Matrix3f matrix, int norm) {
        float normX1 = Normal3b.unpackX(norm);
        float normY1 = Normal3b.unpackY(norm);
        float normZ1 = Normal3b.unpackZ(norm);

        float normX2 = transformVectorX(matrix, normX1, normY1, normZ1);
        float normY2 = transformVectorY(matrix, normX1, normY1, normZ1);
        float normZ2 = transformVectorZ(matrix, normX1, normY1, normZ1);

        return Normal3b.pack(normX2, normY2, normZ2);
    }
}