package net.caffeinemc.sodium.interop.vanilla.math.matrix;

import net.minecraft.util.math.Quaternion;

public interface Matrix4fExtended {
    /**
     * Applies the specified rotation to this matrix in-place.
     *
     * @param quaternion The quaternion to rotate this matrix by
     */
    void rotate(Quaternion quaternion);

    /**
     * Applies this matrix transformation to the given input vector, returning the x-component. Avoids the lack of
     * struct types in Java and allows for allocation-free return.
     * @param x The x-component of the vector
     * @param y The y-component of the vector
     * @param z The z-component of the vector
     * @return The x-component of the transformed input vector
     */
    float transformVecX(float x, float y, float z);

    /**
     * Applies this matrix transformation to the given input vector, returning the y-component. Avoids the lack of
     * struct types in Java and allows for allocation-free return.
     * @param x The x-component of the vector
     * @param y The y-component of the vector
     * @param z The z-component of the vector
     * @return The y-component of the transformed input vector
     */
    float transformVecY(float x, float y, float z);

    /**
     * Applies this matrix transformation to the given input vector, returning the z-component. Avoids the lack of
     * struct types in Java and allows for allocation-free return.
     * @param x The x-component of the vector
     * @param y The y-component of the vector
     * @param z The z-component of the vector
     * @return The z-component of the transformed input vector
     */
    float transformVecZ(float x, float y, float z);

    float getA00();

    void setA00(float value);

    float getA01();

    void setA01(float value);

    float getA02();

    void setA02(float value);

    float getA03();

    void setA03(float value);

    float getA10();

    void setA10(float value);

    float getA11();

    void setA11(float value);

    float getA12();

    void setA12(float value);

    float getA13();

    void setA13(float value);

    float getA20();

    void setA20(float value);

    float getA21();

    void setA21(float value);

    float getA22();

    void setA22(float value);

    float getA23();

    void setA23(float value);

    float getA30();

    void setA30(float value);

    float getA31();

    void setA31(float value);

    float getA32();

    void setA32(float value);

    float getA33();

    void setA33(float value);
}
