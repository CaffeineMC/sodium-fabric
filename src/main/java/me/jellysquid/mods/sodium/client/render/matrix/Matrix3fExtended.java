package me.jellysquid.mods.sodium.client.render.matrix;

import net.minecraft.util.math.Quaternion;

public interface Matrix3fExtended {
    /**
     * Applies the specified rotation to this matrix in-place.
     *
     * @param quaternion The quaternion to rotate this matrix by
     */
    void rotate(Quaternion quaternion);
}
