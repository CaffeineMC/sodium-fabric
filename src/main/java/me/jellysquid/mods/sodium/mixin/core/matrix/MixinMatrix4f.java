package me.jellysquid.mods.sodium.mixin.core.matrix;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;
import me.jellysquid.mods.sodium.client.util.math.Matrix4fExtended;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Quaternion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Matrix4f.class)
public class MixinMatrix4f implements Matrix4fExtended {
    @Shadow
    public float a00;

    @Shadow
    public float a01;

    @Shadow
    public float a02;

    @Shadow
    public float a03;

    @Shadow
    public float a10;

    @Shadow
    public float a11;

    @Shadow
    public float a12;

    @Shadow
    public float a13;

    @Shadow
    public float a20;

    @Shadow
    public float a21;

    @Shadow
    public float a22;

    @Shadow
    public float a23;

    @Shadow
    public float a30;

    @Shadow
    public float a31;

    @Shadow
    public float a32;

    @Shadow
    public float a33;

    @Override
    public void translate(float x, float y, float z) {
        VectorSpecies<Float> SPECIES = FloatVector.SPECIES_128;
        FloatVector v = FloatVector.fromArray(
                SPECIES,
                new float[]{this.a00, this.a10, this.a20, this.a30},
                0
        ).fma(
                FloatVector.broadcast(SPECIES, x),
                FloatVector.fromArray(
                        SPECIES,
                        new float[]{this.a01, this.a11, this.a21, this.a31},
                        0
                ).fma(
                        FloatVector.broadcast(SPECIES, y),
                        FloatVector.fromArray(
                                SPECIES,
                                new float[]{this.a02, this.a12, this.a22, this.a32},
                                0
                        ).fma(
                                FloatVector.broadcast(SPECIES, z),
                                FloatVector.fromArray(
                                        SPECIES,
                                        new float[]{this.a03, this.a13, this.a23, this.a33},
                                        0
                                )
                        )
                )
        );

        this.a03 = v.lane(0);
        this.a13 = v.lane(1);
        this.a23 = v.lane(2);
        this.a33 = v.lane(3);
    }

    @Override
    public float transformVecX(float x, float y, float z) {
//        return (this.a00 * x) + (this.a01 * y) + (this.a02 * z) + (this.a03 * 1.0f);
        return FloatVector.fromArray(
                FloatVector.SPECIES_128,
                new float[]{this.a00, this.a01, this.a02, this.a03},
                0
        ).mul(
                FloatVector.fromArray(
                        FloatVector.SPECIES_128,
                        new float[]{x, y, z, 1.0f},
                        0
                )
        ).reduceLanes(VectorOperators.ADD);
    }

    @Override
    public float transformVecY(float x, float y, float z) {
//        return (this.a10 * x) + (this.a11 * y) + (this.a12 * z) + (this.a13 * 1.0f);
        return FloatVector.fromArray(
                FloatVector.SPECIES_128,
                new float[]{this.a10, this.a11, this.a12, this.a13},
                0
        ).mul(
                FloatVector.fromArray(
                        FloatVector.SPECIES_128,
                        new float[]{x, y, z, 1.0f},
                        0
                )
        ).reduceLanes(VectorOperators.ADD);
    }

    @Override
    public float transformVecZ(float x, float y, float z) {
//        return (this.a20 * x) + (this.a21 * y) + (this.a22 * z) + (this.a23 * 1.0f);
        return FloatVector.fromArray(
                FloatVector.SPECIES_128,
                new float[]{this.a20, this.a21, this.a22, this.a23},
                0
        ).mul(
                FloatVector.fromArray(
                        FloatVector.SPECIES_128,
                        new float[]{x, y, z, 1.0f},
                        0
                )
        ).reduceLanes(VectorOperators.ADD);
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
                new float[]{this.a01, this.a01, this.a11, this.a11, this.a21, this.a21, this.a31, this.a31},
                0
        ).fma(
                SPECIES.fromArray(
                        new float[]{ta11, ta12, ta11, ta12, ta11, ta12, ta11, ta12},
                        0
                ),
                SPECIES.fromArray(
                        new float[]{this.a02, this.a02, this.a12, this.a12, this.a22, this.a22, this.a32, this.a32},
                        0
                ).mul(
                        SPECIES.fromArray(
                                new float[]{ta21, ta22, ta21, ta22, ta21, ta22, ta21, ta22},
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
        this.a31 = v.lane(6);
        this.a32 = v.lane(7);
    }

    private void rotateY(Quaternion quaternion) {
        float y = quaternion.getY();
        float w = quaternion.getW();

        float yy = 2.0F * y * y;
        float ta00 = 1.0F - yy;
        float ta22 = 1.0F - yy;
        float yw = y * w;
        float ta20 = 2.0F * -yw;
        float ta02 = 2.0F * yw;

        VectorSpecies<Float> SPECIES = FloatVector.SPECIES_256;
        FloatVector v = FloatVector.fromArray(
                SPECIES,
                new float[]{this.a00, this.a00, this.a10, this.a10, this.a20, this.a20, this.a30, this.a30},
                0
        ).fma(
                SPECIES.fromArray(
                        new float[]{ta00, ta02, ta00, ta02, ta00, ta02, ta00, ta02},
                        0
                ),
                SPECIES.fromArray(
                        new float[]{this.a02, this.a02, this.a12, this.a12, this.a22, this.a22, this.a32, this.a32},
                        0
                ).mul(
                        SPECIES.fromArray(
                                new float[]{ta20, ta22, ta20, ta22, ta20, ta22, ta20, ta22},
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
        this.a30 = v.lane(6);
        this.a32 = v.lane(7);
    }

    private void rotateZ(Quaternion quaternion) {
        float z = quaternion.getZ();
        float w = quaternion.getW();

        float zz = 2.0F * z * z;
        float ta00 = 1.0F - zz;
        float ta11 = 1.0F - zz;
        float zw = z * w;
        float ta10 = 2.0F * zw;
        float ta01 = 2.0F * -zw;

        VectorSpecies<Float> SPECIES = FloatVector.SPECIES_256;
        FloatVector v = FloatVector.fromArray(
                SPECIES,
                new float[]{this.a00, this.a00, this.a10, this.a10, this.a20, this.a20, this.a30, this.a30},
                0
        ).fma(
                SPECIES.fromArray(
                        new float[]{ta00, ta01, ta00, ta01, ta00, ta01, ta00, ta01},
                        0
                ),
                SPECIES.fromArray(
                        new float[]{this.a01, this.a01, this.a11, this.a11, this.a21, this.a21, this.a31, this.a31},
                        0
                ).mul(
                        SPECIES.fromArray(
                                new float[]{ta10, ta11, ta10, ta11, ta10, ta11, ta10, ta11},
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
        this.a30 = v.lane(6);
        this.a31 = v.lane(7);
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

        VectorSpecies<Float> SPECIES_256 = FloatVector.SPECIES_256;
        VectorSpecies<Float> SPECIES_128 = FloatVector.SPECIES_128;
        float[] a = new float[12];
        FloatVector.fromArray(
                SPECIES_256,
                new float[]{this.a00, this.a00, this.a00, this.a10, this.a10, this.a10, this.a20, this.a20},
                0
        ).fma(
                SPECIES_256.fromArray(
                        new float[]{ta00, ta01, ta02, ta00, ta01, ta02, ta00, ta01},
                        0
                ),
                FloatVector.fromArray(
                        SPECIES_256,
                        new float[]{this.a01, this.a01, this.a01, this.a11, this.a11, this.a11, this.a21, this.a21},
                        0
                ).fma(
                        SPECIES_256.fromArray(
                                new float[]{ta10, ta11, ta12, ta10, ta11, ta12, ta10, ta11},
                                0
                        ),
                        SPECIES_256.fromArray(
                                new float[]{this.a02, this.a02, this.a02, this.a12, this.a12, this.a12, this.a22, this.a22},
                                0
                        ).mul(
                                SPECIES_256.fromArray(
                                        new float[]{ta20, ta21, ta22, ta20, ta21, ta22, ta20, ta21},
                                        0
                                )
                        )
                )
        ).intoArray(a, 0);

        FloatVector.fromArray(
                SPECIES_128,
                new float[]{this.a20, this.a30, this.a30, this.a30},
                0
        ).fma(
                SPECIES_128.fromArray(
                        new float[]{ta02, ta00, ta01, ta02},
                        0
                ),
                FloatVector.fromArray(
                        SPECIES_128,
                        new float[]{this.a21, this.a31, this.a31, this.a31},
                        0
                ).fma(
                        SPECIES_128.fromArray(
                                new float[]{ta12, ta10, ta11, ta12},
                                0
                        ),
                        SPECIES_128.fromArray(
                                new float[]{this.a22, this.a32, this.a32, this.a32},
                                0
                        ).mul(
                                SPECIES_128.fromArray(
                                        new float[]{ta22, ta20, ta21, ta22},
                                        0
                                )
                        )
                )
        ).intoArray(a, 8);

        this.a00 = a[0];
        this.a01 = a[1];
        this.a02 = a[2];
        this.a10 = a[3];
        this.a11 = a[4];
        this.a12 = a[5];
        this.a20 = a[6];
        this.a21 = a[7];
        this.a22 = a[8];
        this.a30 = a[9];
        this.a31 = a[10];
        this.a32 = a[11];
    }
}
