package me.jellysquid.mods.sodium.common.util.matrix;

import net.minecraft.util.math.Quaternion;

public interface Matrix4fExtended {
    /**
     * Applies the specified rotation to this matrix in-place.
     *
     * @param quaternion The quaternion to rotate this matrix by
     */
    void rotate(Quaternion quaternion);

    /**
     * Applies the specified translation to this matrix in-place. The implementation can take advantage
     *
     * @param x The x-component of the translation
     * @param y The y-component of the translation
     * @param z The z-component of the translation
     */
    void translate(float x, float y, float z);
}
