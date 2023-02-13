package net.caffeinemc.mods.sodium.api.math;

import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class MatrixHelper {
    /**
     * @param mat The transformation matrix to apply to the normal
     * @param x The X-coordinate of the normal vector
     * @param y The Y-coordinate of the normal vector
     * @param z The Z-coordinate of the normal vector
     * @return The transformed normal vector (in packed format)
     */
    public static int transformNormal(Matrix3f mat, float x, float y, float z) {
        // The transformed normal vector
        float nxt = transformNormalX(mat, x, y, z);
        float nyt = transformNormalY(mat, x, y, z);
        float nzt = transformNormalZ(mat, x, y, z);

        return NormI8.pack(nxt, nyt, nzt);
    }

    /**
     * @param mat The transformation matrix to apply to the normal
     * @param norm The normal vector to transform (in packed format)
     * @return The transformed normal vector (in packed format)
     */
    public static int transformNormal(Matrix3f mat, int norm) {
        // The unpacked normal vector
        float x = NormI8.unpackX(norm);
        float y = NormI8.unpackY(norm);
        float z = NormI8.unpackZ(norm);

        return transformNormal(mat, x, y, z);
    }

    /**
     * @param mat The transformation matrix to apply to the normal vector
     * @param x The X-coordinate of the normal vector
     * @param y The Y-coordinate of the normal vector
     * @param z The Z-coordinate of the normal vector
     * @return The transformed X-coordinate for the normal vector
     */
    public static float transformNormalX(Matrix3f mat, float x, float y, float z) {
        return (mat.m00() * x) + (mat.m10() * y) + (mat.m20() * z);
    }

    /**
     * @param mat The transformation matrix to apply to the normal vector
     * @param x The X-coordinate of the normal vector
     * @param y The Y-coordinate of the normal vector
     * @param z The Z-coordinate of the normal vector
     * @return The transformed Y-coordinate for the normal vector
     */
    public static float transformNormalY(Matrix3f mat, float x, float y, float z) {
        return (mat.m01() * x) + (mat.m11() * y) + (mat.m21() * z);
    }

    /**
     * @param mat The transformation matrix to apply to the normal vector
     * @param x The X-coordinate of the normal vector
     * @param y The Y-coordinate of the normal vector
     * @param z The Z-coordinate of the normal vector
     * @return The transformed Z-coordinate for the normal vector
     */
    public static float transformNormalZ(Matrix3f mat, float x, float y, float z) {
        return (mat.m02() * x) + (mat.m12() * y) + (mat.m22() * z);
    }

    /**
     * @param mat The transformation matrix to apply to the vertex position
     * @param x The X-coordinate of the vertex position
     * @param y The Y-coordinate of the vertex position
     * @param z The Z-coordinate of the vertex position
     * @return The transformed X-coordinate for the vertex position
     */
    public static float transformPositionX(Matrix4f mat, float x, float y, float z) {
        return (mat.m00() * x) + (mat.m10() * y) + (mat.m20() * z) + mat.m30();
    }

    /**
     * @param mat The transformation matrix to apply to the vertex position
     * @param x The X-coordinate of the vertex position
     * @param y The Y-coordinate of the vertex position
     * @param z The Z-coordinate of the vertex position
     * @return The transformed Y-coordinate for the vertex position
     */
    public static float transformPositionY(Matrix4f mat, float x, float y, float z) {
        return (mat.m01() * x) + (mat.m11() * y) + (mat.m21() * z) + mat.m31();
    }

    /**
     * @param mat The transformation matrix to apply to the vertex position
     * @param x The X-coordinate of the vertex position
     * @param y The Y-coordinate of the vertex position
     * @param z The Z-coordinate of the vertex position
     * @return The transformed Z-coordinate for the vertex position
     */
    public static float transformPositionZ(Matrix4f mat, float x, float y, float z) {
        return (mat.m02() * x) + (mat.m12() * y) + (mat.m22() * z) + mat.m32();
    }

    /**
     * Rotates the position and normal matrix in ZYX order. The rotation angles are specified in radians. This is
     * functionally identical to rotating the matrix stack by a quaternion representing an ZYX rotation, but is
     * significantly faster.
     *
     * @param matrices The matrix stack to rotate
     * @param angleZ The angle to rotate by on the Z-axis
     * @param angleY The angle to rotate by on the Y-axis
     * @param angleX The angle to rotate by on the X-axis
     */
    public static void rotateZYX(MatrixStack.Entry matrices, float angleZ, float angleY, float angleX) {
        float sinX = Math.sin(angleX);
        float cosX = Math.cosFromSin(sinX, angleX);
        float sinInvX = -sinX;

        float sinY = Math.sin(angleY);
        float cosY = Math.cosFromSin(sinY, angleY);
        float sinInvY = -sinY;

        float sinZ = Math.sin(angleZ);
        float cosZ = Math.cosFromSin(sinZ, angleZ);
        float sinInvZ = -sinZ;

        applySinCosMat4(matrices.getPositionMatrix(), sinX, sinY, sinZ, cosX, cosY, cosZ, sinInvX, sinInvY, sinInvZ);
        applySinCosMat3(matrices.getNormalMatrix(), sinX, sinY, sinZ, cosX, cosY, cosZ, sinInvX, sinInvY, sinInvZ);
    }

    private static void applySinCosMat4(Matrix4f mat, float sinX, float sinY, float sinZ, float cosX, float cosY, float cosZ, float sinInvX, float sinInvY, float sinInvZ) {
        float nm00 = (mat.m00() * cosZ) + (mat.m10() * sinZ);
        float nm01 = (mat.m01() * cosZ) + (mat.m11() * sinZ);
        float nm02 = (mat.m02() * cosZ) + (mat.m12() * sinZ);
        float nm03 = (mat.m03() * cosZ) + (mat.m13() * sinZ);

        float nm10 = (mat.m00() * sinInvZ) + (mat.m10() * cosZ);
        float nm11 = (mat.m01() * sinInvZ) + (mat.m11() * cosZ);
        float nm12 = (mat.m02() * sinInvZ) + (mat.m12() * cosZ);
        float nm13 = (mat.m03() * sinInvZ) + (mat.m13() * cosZ);

        float nm20 = (nm00 * sinY) + (mat.m20() * cosY);
        float nm21 = (nm01 * sinY) + (mat.m21() * cosY);
        float nm22 = (nm02 * sinY) + (mat.m22() * cosY);
        float nm23 = (nm03 * sinY) + (mat.m23() * cosY);

        // Setting each component individually involves significant overhead since the properties
        // for the matrix will be re-calculated each time.
        mat.set(
                (nm00 * cosY) + (mat.m20() * sinInvY),
                (nm01 * cosY) + (mat.m21() * sinInvY),
                (nm02 * cosY) + (mat.m22() * sinInvY),
                (nm03 * cosY) + (mat.m23() * sinInvY),

                (nm10 * cosX) + (nm20 * sinX),
                (nm11 * cosX) + (nm21 * sinX),
                (nm12 * cosX) + (nm22 * sinX),
                (nm13 * cosX) + (nm23 * sinX),

                (nm10 * sinInvX) + (nm20 * cosX),
                (nm11 * sinInvX) + (nm21 * cosX),
                (nm12 * sinInvX) + (nm22 * cosX),
                (nm13 * sinInvX) + (nm23 * cosX),

                mat.m30(),
                mat.m31(),
                mat.m32(),
                mat.m33()
        );
    }

    private static void applySinCosMat3(Matrix3f mat, float sinX, float sinY, float sinZ, float cosX, float cosY, float cosZ, float sinInvX, float sinInvY, float sinInvZ) {
        float nm00 = mat.m00() * cosZ + mat.m10() * sinZ;
        float nm01 = mat.m01() * cosZ + mat.m11() * sinZ;
        float nm02 = mat.m02() * cosZ + mat.m12() * sinZ;

        float nm10 = mat.m00() * sinInvZ + mat.m10() * cosZ;
        float nm11 = mat.m01() * sinInvZ + mat.m11() * cosZ;
        float nm12 = mat.m02() * sinInvZ + mat.m12() * cosZ;

        float nm20 = nm00 * sinY + mat.m20() * cosY;
        float nm21 = nm01 * sinY + mat.m21() * cosY;
        float nm22 = nm02 * sinY + mat.m22() * cosY;

        // Setting each component individually involves significant overhead since the properties
        // for the matrix will be re-calculated each time.
        mat.set(nm00 * cosY + mat.m20() * sinInvY,
                nm01 * cosY + mat.m21() * sinInvY,
                nm02 * cosY + mat.m22() * sinInvY,

                nm10 * cosX + nm20 * sinX,
                nm11 * cosX + nm21 * sinX,
                nm12 * cosX + nm22 * sinX,

                nm10 * sinInvX + nm20 * cosX,
                nm11 * sinInvX + nm21 * cosX,
                nm12 * sinInvX + nm22 * cosX);
    }
}
