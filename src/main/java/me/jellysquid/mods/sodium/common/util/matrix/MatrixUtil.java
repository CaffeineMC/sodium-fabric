package me.jellysquid.mods.sodium.common.util.matrix;

import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Matrix3f;

public class MatrixUtil {
    public static int computeNormal(Matrix3f normalMatrix, Direction facing) {
        return ((Matrix3fExtended) (Object) normalMatrix).computeNormal(facing);
    }
}
