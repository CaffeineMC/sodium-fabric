package net.minecraft.client.util.math;

public class Sodium_Matrix4fUtil {
    public static float mulX(float x, float y, float z, Matrix4f matrix) {
        return matrix.a00 * x + matrix.a01 * y + matrix.a02 * z + matrix.a03;
    }

    public static float mulY(float x, float y, float z, Matrix4f matrix) {
        return matrix.a10 * x + matrix.a11 * y + matrix.a12 * z + matrix.a13;
    }

    public static float mulZ(float x, float y, float z, Matrix4f matrix) {
        return matrix.a20 * x + matrix.a21 * y + matrix.a22 * z + matrix.a23;
    }

    public static void copy(Matrix4f src, Matrix4f dst) {
        dst.a00 = src.a00;
        dst.a01 = src.a01;
        dst.a02 = src.a02;
        dst.a03 = src.a03;

        dst.a10 = src.a10;
        dst.a11 = src.a11;
        dst.a12 = src.a12;
        dst.a13 = src.a13;

        dst.a20 = src.a20;
        dst.a21 = src.a21;
        dst.a22 = src.a22;
        dst.a23 = src.a23;

        dst.a30 = src.a30;
        dst.a31 = src.a31;
        dst.a32 = src.a32;
        dst.a33 = src.a33;
    }

    public static void setTranslation(Matrix4f matrix, float x, float y, float z) {
        matrix.a00 = 1.0F;
        matrix.a11 = 1.0F;
        matrix.a22 = 1.0F;
        matrix.a33 = 1.0F;
        matrix.a03 = x;
        matrix.a13 = y;
        matrix.a23 = z;
    }
}
