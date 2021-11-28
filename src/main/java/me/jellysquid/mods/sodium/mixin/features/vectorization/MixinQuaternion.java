package me.jellysquid.mods.sodium.mixin.features.vectorization;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorSpecies;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Quaternion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Quaternion.class)
public class MixinQuaternion {
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
    public void hamiltonProduct(Quaternion other) {
        float f = this.x;
        float g = this.y;
        float h = this.z;
        float i = this.w;
        float j = other.getX();
        float k = other.getY();
        float l = other.getZ();
        float m = other.getW();

        float[] q = FloatVector.fromArray(
                S_128,
                new float[]{j, k, l, m},
                0
        ).mul(i).add(
                FloatVector.fromArray(
                        S_128,
                        new float[]{m, -l, k, -j},
                        0
                ).mul(f)
        ).add(
                FloatVector.fromArray(
                        S_128,
                        new float[]{l, m, -j, -k},
                        0
                ).mul(g)
        ).add(
                FloatVector.fromArray(
                        S_128,
                        new float[]{-k, j, m, -l},
                        0
                ).mul(h)
        ).toArray();

        this.x = q[0];
        this.y = q[1];
        this.z = q[2];
        this.w = q[3];
    }

    /**
     * @author Vonr
     * @reason Use Vector API
     */
    @Overwrite
    public void scale(float scale) {
        float[] q = FloatVector.fromArray(
                S_128,
                new float[]{this.x, this.y, this.z, this.w},
                0
        ).mul(scale).toArray();

        this.x = q[0];
        this.y = q[1];
        this.z = q[2];
        this.w = q[3];
    }

    /**
     * @author Vonr
     * @reason Use Vector API
     */
    @Overwrite
    public void conjugate() {
        float[] q = FloatVector.fromArray(
                S_128,
                new float[]{this.x, this.y, this.z, 0},
                0
        ).neg().toArray();

        this.x = q[0];
        this.y = q[1];
        this.z = q[2];
    }

    /**
     * @author Vonr
     * @reason Use Vector API
     */
    @Overwrite
    public void normalize() {
        float f = this.x * this.x + this.y * this.y + this.z * this.z + this.w * this.w;
        float[] q = FloatVector.fromArray(
                S_128,
                new float[]{this.x, this.y, this.z, this.w},
                0
        ).mul((f > 1.0E-6F) ? MathHelper.fastInverseSqrt(f) : 0).toArray();

        this.x = q[0];
        this.y = q[1];
        this.z = q[2];
        this.w = q[3];
    }
}
