package me.jellysquid.mods.sodium.mixin.features.vectorization;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Vector4f.class)
public class MixinVector4f {
    @Shadow
    private float x;

    @Shadow
    private float y;

    @Shadow
    private float z;

    @Shadow
    private float w;

    @Unique
    VectorSpecies<Float> S_128 = FloatVector.SPECIES_128;

    /**
     * @author Vonr
     * @reason Use Vector API
     */
    @Overwrite
    public float dotProduct(Vector4f other) {
        return FloatVector.fromArray(
                S_128,
                new float[]{this.x, this.y, this.z, this.w},
                0
        ).mul(
                FloatVector.fromArray(
                        S_128,
                        new float[]{other.getX(), other.getY(), other.getZ(), other.getW()},
                        0
                )
        ).reduceLanes(VectorOperators.ADD);
    }

    /**
     * @author Vonr
     * @reason Use Vector API
     */
    @Overwrite
    public boolean normalize() {
        FloatVector v = FloatVector.fromArray(
                S_128,
                new float[]{this.x, this.y, this.z, this.w},
                0
        );
        float f = v.mul(v).reduceLanes(VectorOperators.ADD);
        if (f < 1.0E-5F) {
            return false;
        } else {
            float[] q = v.mul(MathHelper.fastInverseSqrt(f)).toArray();

            this.x = q[0];
            this.y = q[1];
            this.z = q[2];
            this.w = q[3];
            return true;
        }
    }

    /**
     * @author Vonr
     * @reason Use Vector API
     */
    @Overwrite
    public void transform(Matrix4f matrix) {
        FloatVector v = FloatVector.fromArray(
                S_128,
                new float[]{this.x, this.y, this.z, this.w},
                0
        );

        this.x = FloatVector.fromArray(
                S_128,
                new float[]{matrix.a00, matrix.a01, matrix.a02, matrix.a03},
                0
        ).mul(v).reduceLanes(VectorOperators.ADD);

        this.y = FloatVector.fromArray(
                S_128,
                new float[]{matrix.a10, matrix.a11, matrix.a12, matrix.a13},
                0
        ).mul(v).reduceLanes(VectorOperators.ADD);

        this.z = FloatVector.fromArray(
                S_128,
                new float[]{matrix.a20, matrix.a21, matrix.a22, matrix.a23},
                0
        ).mul(v).reduceLanes(VectorOperators.ADD);

        this.w = FloatVector.fromArray(
                S_128,
                new float[]{matrix.a30, matrix.a31, matrix.a32, matrix.a33},
                0
        ).mul(v).reduceLanes(VectorOperators.ADD);
    }
}
