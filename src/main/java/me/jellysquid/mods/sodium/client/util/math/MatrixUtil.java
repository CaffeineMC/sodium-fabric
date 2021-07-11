package me.jellysquid.mods.sodium.client.util.math;

import me.jellysquid.mods.sodium.client.util.Norm3b;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3i;

@SuppressWarnings("ConstantConditions")
public class MatrixUtil {
    public static int computeNormal(Matrix3f normalMatrix, Direction facing) {
        return ((Matrix3fExtended) (Object) normalMatrix).computeNormal(facing);
    }

    public static Matrix4fExtended getExtendedMatrix(Matrix4f matrix) {
        return (Matrix4fExtended) (Object) matrix;
    }

    public static Matrix3fExtended getExtendedMatrix(Matrix3f matrix) {
        return (Matrix3fExtended) (Object) matrix;
    }

    public static int transformNormalVector(Vec3i vector, Matrix3f matrix) {
        Matrix3fExtended mat = MatrixUtil.getExtendedMatrix(matrix);

        float x = vector.getX();
        float y = vector.getY();
        float z = vector.getZ();

        return Norm3b.pack(
                mat.transformVecX(x, y, z),
                mat.transformVecY(x, y, z),
                mat.transformVecZ(x, y, z)
        );
    }
}
