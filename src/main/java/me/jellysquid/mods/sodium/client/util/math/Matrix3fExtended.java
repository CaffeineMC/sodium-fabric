package me.jellysquid.mods.sodium.client.util.math;

import net.minecraft.client.util.math.Vector3f;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Quaternion;

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

    default float transformVecX(Vector3f dir) {
        return this.transformVecX(dir.getX(), dir.getY(), dir.getZ());
    }

    default float transformVecY(Vector3f dir) {
        return this.transformVecY(dir.getX(), dir.getY(), dir.getZ());
    }

    default float transformVecZ(Vector3f dir) {
        return this.transformVecZ(dir.getX(), dir.getY(), dir.getZ());
    }
}
