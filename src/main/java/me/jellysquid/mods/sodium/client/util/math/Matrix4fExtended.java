package me.jellysquid.mods.sodium.client.util.math;

import net.minecraft.client.util.math.Vector3f;
import net.minecraft.util.math.Quaternion;

public interface Matrix4fExtended {
    /**
     * Applies the specified rotation to this matrix in-place.
     *
     * @param quaternion The quaternion to rotate this matrix by
     */
    void rotate(Quaternion quaternion);

    /**
     * Applies the specified translation to this matrix in-place.
     *
     * @param x The x-component of the translation
     * @param y The y-component of the translation
     * @param z The z-component of the translation
     */
    void translate(float x, float y, float z);

    float transformVecX(float x, float y, float z);

    float transformVecY(float x, float y, float z);

    float transformVecZ(float x, float y, float z);

    default float transformVecX(Vector3f pos) {
        return this.transformVecX(pos.getX(), pos.getY(), pos.getZ());
    }

    default float transformVecY(Vector3f pos) {
        return this.transformVecY(pos.getX(), pos.getY(), pos.getZ());
    }

    default float transformVecZ(Vector3f pos) {
        return this.transformVecZ(pos.getX(), pos.getY(), pos.getZ());
    }
}
