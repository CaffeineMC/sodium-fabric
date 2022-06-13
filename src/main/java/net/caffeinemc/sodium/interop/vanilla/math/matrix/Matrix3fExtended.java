package net.caffeinemc.sodium.interop.vanilla.math.matrix;

import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3f;

import java.nio.FloatBuffer;

public interface Matrix3fExtended {
    /**
     * Applies the specified rotation to this matrix in-place.
     *
     * @param quaternion The quaternion to rotate this matrix by
     */
    void rotate(Quaternion quaternion);

    int computeNormal(Direction dir);

    float transformVecX(float x, float y, float z);

    float transformVecY(float x, float y, float z);

    float transformVecZ(float x, float y, float z);

    default float transformVecX(Vec3f dir) {
        return this.transformVecX(dir.getX(), dir.getY(), dir.getZ());
    }

    default float transformVecY(Vec3f dir) {
        return this.transformVecY(dir.getX(), dir.getY(), dir.getZ());
    }

    default float transformVecZ(Vec3f dir) {
        return this.transformVecZ(dir.getX(), dir.getY(), dir.getZ());
    }

    float getA00();

    void setA00(float value);

    float getA10();

    void setA10(float value);

    float getA20();

    void setA20(float value);

    float getA01();

    void setA01(float value);

    float getA11();

    void setA11(float value);

    float getA21();

    void setA21(float value);

    float getA02();

    void setA02(float value);

    float getA12();

    void setA12(float value);

    float getA22();

    void setA22(float value);

    void writeColumnMajor3x4(FloatBuffer buffer);
}
