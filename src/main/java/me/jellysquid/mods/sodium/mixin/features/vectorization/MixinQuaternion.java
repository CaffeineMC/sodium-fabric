package me.jellysquid.mods.sodium.mixin.features.vectorization;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorOperators;
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
        float j = other.getX();
        float k = other.getY();
        float l = other.getZ();
        float m = other.getW();

        FloatVector v = FloatVector.fromArray(
                S_128,
                new float[]{j, k, l, m},
                0
        ).fma(
                FloatVector.broadcast(S_128, this.w),
                FloatVector.fromArray(
                        S_128,
                        new float[]{m, -l, k, -j},
                        0
                ).fma(
                        FloatVector.broadcast(S_128, this.x),
                        FloatVector.fromArray(
                                S_128,
                                new float[]{l, m, -j, -k},
                                0
                        ).fma(
                                FloatVector.broadcast(S_128, this.y),
                                FloatVector.fromArray(
                                        S_128,
                                        new float[]{-k, j, m, -l},
                                        0
                                ).mul(this.z)
                        )
                )
        );

        this.x = v.lane(0);
        this.y = v.lane(1);
        this.z = v.lane(2);
        this.w = v.lane(3);
    }

    /**
     * @author Vonr
     * @reason Use Vector API
     */
    @Overwrite
    public void scale(float scale) {
        FloatVector v = FloatVector.fromArray(
                S_128,
                new float[]{this.x, this.y, this.z, this.w},
                0
        ).mul(scale);

        this.x = v.lane(0);
        this.y = v.lane(1);
        this.z = v.lane(2);
        this.w = v.lane(3);
    }

    /**
     * @author Vonr
     * @reason Use Vector API
     */
    @Overwrite
    public void normalize() {
        FloatVector v = FloatVector.fromArray(
                S_128,
                new float[]{this.x, this.y, this.z, this.w},
                0
        );
        float f = v.mul(v).reduceLanes(VectorOperators.ADD);
        if (f < 1.0E-6F) {
            FloatVector v2 = v.mul(MathHelper.fastInverseSqrt(f));
            this.x = v2.lane(0);
            this.y = v2.lane(1);
            this.z = v2.lane(2);
            this.w = v2.lane(3);
            return;
        }

        this.x = 0;
        this.y = 0;
        this.z = 0;
        this.w = 0;
    }
}
