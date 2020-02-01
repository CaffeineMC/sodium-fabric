package net.minecraft.client.util.math;

public class Sodium_Matrix3fUtil {
    public static float mulX(float x, float y, float z, Matrix3f mat) {
        return mat.a00 * x + mat.a01 * y + mat.a02 * z;
    }

    public static float mulY(float x, float y, float z, Matrix3f mat) {
        return mat.a10 * x + mat.a11 * y + mat.a12 * z;
    }

    public static float mulZ(float x, float y, float z, Matrix3f mat) {
        return mat.a20 * x + mat.a21 * y + mat.a22 * z;
    }

    public static void copy(Matrix3f src, Matrix3f dst) {
        dst.a00 = src.a00;
        dst.a01 = src.a01;
        dst.a02 = src.a02;

        dst.a10 = src.a10;
        dst.a11 = src.a11;
        dst.a12 = src.a12;

        dst.a20 = src.a20;
        dst.a21 = src.a21;
        dst.a22 = src.a22;
    }
}
