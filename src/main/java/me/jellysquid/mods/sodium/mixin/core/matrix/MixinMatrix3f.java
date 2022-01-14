package me.jellysquid.mods.sodium.mixin.core.matrix;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorSpecies;
import me.jellysquid.mods.sodium.client.util.Norm3b;
import me.jellysquid.mods.sodium.client.util.math.Matrix3fExtended;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Matrix3f.class)
public class MixinMatrix3f implements Matrix3fExtended {
    @Shadow
    protected float a00;

    @Shadow
    protected float a10;

    @Shadow
    protected float a20;

    @Shadow
    protected float a01;

    @Shadow
    protected float a11;

    @Shadow
    protected float a21;

    @Shadow
    protected float a02;

    @Shadow
    protected float a12;

    @Shadow
    protected float a22;

    @Override
    public float transformVecX(float x, float y, float z) {
        return this.a00 * x + this.a01 * y + this.a02 * z;
    }

    @Override
    public float transformVecY(float x, float y, float z) {
        return this.a10 * x + this.a11 * y + this.a12 * z;
    }

    @Override
    public float transformVecZ(float x, float y, float z) {
        return this.a20 * x + this.a21 * y + this.a22 * z;
    }

    @Override
    public void rotate(Quaternion quaternion) {
        boolean x = quaternion.getX() != 0.0F;
        boolean y = quaternion.getY() != 0.0F;
        boolean z = quaternion.getZ() != 0.0F;

        // Try to determine if this is a simple rotation on one axis component only
        if (x) {
            if (!y && !z) {
                this.rotateX(quaternion);
            } else {
                this.rotateXYZ(quaternion);
            }
        } else if (y) {
            if (!z) {
                this.rotateY(quaternion);
            } else {
                this.rotateXYZ(quaternion);
            }
        } else if (z) {
            this.rotateZ(quaternion);
        }
    }

    @Override
    public int computeNormal(Direction dir) {
        Vec3i faceNorm = dir.getVector();

        float x = faceNorm.getX();
        float y = faceNorm.getY();
        float z = faceNorm.getZ();

        float x2 = this.a00 * x + this.a01 * y + this.a02 * z;
        float y2 = this.a10 * x + this.a11 * y + this.a12 * z;
        float z2 = this.a20 * x + this.a21 * y + this.a22 * z;

        return Norm3b.pack(x2, y2, z2);
    }

    private void rotateX(Quaternion quaternion) {
        float x = quaternion.getX();
        float w = quaternion.getW();

        float xx = 2.0F * x * x;

        float ta11 = 1.0F - xx;
        float ta22 = 1.0F - xx;

        float xw = x * w;
        float ta21 = 2.0F * xw;
        float ta12 = 2.0F * -xw;

        VectorSpecies<Float> SPECIES = FloatVector.SPECIES_256;
        FloatVector v = FloatVector.fromArray(
                SPECIES,
                new float[]{this.a01, this.a01, this.a11, this.a11, this.a21, this.a21, 0,0},
                0
        ).fma(
                FloatVector.fromArray(
                        SPECIES,
                        new float[]{ta11, ta12, ta11, ta12, ta11, ta12, 0,0},
                        0
                ),
                FloatVector.fromArray(
                        SPECIES,
                        new float[]{this.a02, this.a02, this.a12, this.a12, this.a22, this.a22, 0,0},
                        0
                ).mul(
                        FloatVector.fromArray(
                                SPECIES,
                                new float[]{ta21, ta22, ta21, ta22, ta21, ta22, 0,0},
                                0
                        )
                )
        );

        this.a01 = v.lane(0);
        this.a02 = v.lane(1);
        this.a11 = v.lane(2);
        this.a12 = v.lane(3);
        this.a21 = v.lane(4);
        this.a22 = v.lane(5);
    }

    private void rotateY(Quaternion quaternion) {
        float y = quaternion.getY();
        float w = quaternion.getW();

        float yy = 2.0F * y * y;

        float ta00 = 1.0F - yy;
        float ta22 = 1.0F - yy;

        float yw = y * w;

        float ta20 = 2.0F * (-yw);
        float ta02 = 2.0F * (+yw);

        VectorSpecies<Float> SPECIES = FloatVector.SPECIES_256;
        FloatVector v = FloatVector.fromArray(
                SPECIES,
                new float[]{this.a00, this.a00, this.a10, this.a10, this.a20, this.a20, 0,0},
                0
        ).fma(
                SPECIES.fromArray(
                        new float[]{ta00, ta02, ta00, ta02, ta00, ta02, 0,0},
                        0
                ),
                SPECIES.fromArray(
                        new float[]{this.a02, this.a02, this.a12, this.a12, this.a22, this.a22, 0,0},
                        0
                ).mul(
                        SPECIES.fromArray(
                                new float[]{ta20, ta22, ta20, ta22, ta20, ta22, 0,0},
                                0
                        )
                )
        );

        this.a00 = v.lane(0);
        this.a02 = v.lane(1);
        this.a10 = v.lane(2);
        this.a12 = v.lane(3);
        this.a20 = v.lane(4);
        this.a22 = v.lane(5);
    }

    private void rotateZ(Quaternion quaternion) {
        float z = quaternion.getZ();
        float w = quaternion.getW();

        float zz = 2.0F * z * z;

        float ta00 = 1.0F - zz;
        float ta11 = 1.0F - zz;

        float zw = z * w;

        float ta10 = 2.0F * (0.0F + zw);
        float ta01 = 2.0F * (0.0F - zw);

        VectorSpecies<Float> SPECIES = FloatVector.SPECIES_256;
        FloatVector v = FloatVector.fromArray(
                SPECIES,
                new float[]{this.a00, this.a00, this.a10, this.a10, this.a20, this.a20, 0,0},
                0
        ).fma(
                SPECIES.fromArray(
                        new float[]{ta00, ta01, ta00, ta01, ta00, ta01, 0,0},
                        0
                ),
                SPECIES.fromArray(
                        new float[]{this.a01, this.a01, this.a11, this.a11, this.a21, this.a21, 0,0},
                        0
                ).mul(
                        SPECIES.fromArray(
                                new float[]{ta10, ta11, ta10, ta11, ta10, ta11, 0,0},
                                0
                        )
                )
        );

        this.a00 = v.lane(0);
        this.a01 = v.lane(1);
        this.a10 = v.lane(2);
        this.a11 = v.lane(3);
        this.a20 = v.lane(4);
        this.a21 = v.lane(5);
    }

    private void rotateXYZ(Quaternion quaternion) {
        float x = quaternion.getX();
        float y = quaternion.getY();
        float z = quaternion.getZ();
        float w = quaternion.getW();

        float xx = 2.0F * x * x;
        float yy = 2.0F * y * y;
        float zz = 2.0F * z * z;

        float ta00 = 1.0F - yy - zz;
        float ta11 = 1.0F - zz - xx;
        float ta22 = 1.0F - xx - yy;

        float xy = x * y;
        float yz = y * z;
        float zx = z * x;
        float xw = x * w;
        float yw = y * w;
        float zw = z * w;

        float ta10 = 2.0F * (xy + zw);
        float ta01 = 2.0F * (xy - zw);
        float ta20 = 2.0F * (zx - yw);
        float ta02 = 2.0F * (zx + yw);
        float ta21 = 2.0F * (yz + xw);
        float ta12 = 2.0F * (yz - xw);

        VectorSpecies<Float> SPECIES = FloatVector.SPECIES_256;
        FloatVector v = FloatVector.fromArray(
                SPECIES,
                new float[]{this.a00, this.a00, this.a00, this.a10, this.a10, this.a10, this.a20, this.a20},
                0
        ).fma(
                SPECIES.fromArray(
                        new float[]{ta00, ta01, ta02, ta00, ta01, ta02, ta00, ta01},
                        0
                ),
                FloatVector.fromArray(
                        SPECIES,
                        new float[]{this.a01, this.a01, this.a01, this.a11, this.a11, this.a11, this.a21, this.a21},
                        0
                ).fma(
                        SPECIES.fromArray(
                                new float[]{ta10, ta11, ta12, ta10, ta11, ta12, ta10, ta11},
                                0
                        ),
                        SPECIES.fromArray(
                                new float[]{this.a02, this.a02, this.a02, this.a12, this.a12, this.a12, this.a22, this.a22},
                                0
                        ).mul(
                                SPECIES.fromArray(
                                        new float[]{ta20, ta21, ta22, ta20, ta21, ta22, ta20, ta21},
                                        0
                                )
                        )
                )
        );
        float a22 = this.a20 * ta02 + this.a21 * ta12 + this.a22 * ta22;

        this.a00 = v.lane(0);
        this.a01 = v.lane(1);
        this.a02 = v.lane(2);
        this.a10 = v.lane(3);
        this.a11 = v.lane(4);
        this.a12 = v.lane(5);
        this.a20 = v.lane(6);
        this.a21 = v.lane(7);
        this.a22 = a22;
    }


    @Override
    public float getA00() {
        return this.a00;
    }

    @Override
    public float getA10() {
        return this.a10;
    }

    @Override
    public float getA20() {
        return this.a20;
    }

    @Override
    public float getA01() {
        return this.a01;
    }

    @Override
    public float getA11() {
        return this.a11;
    }

    @Override
    public float getA21() {
        return this.a21;
    }

    @Override
    public float getA02() {
        return this.a02;
    }

    @Override
    public float getA12() {
        return this.a12;
    }

    @Override
    public float getA22() {
        return this.a22;
    }
}
