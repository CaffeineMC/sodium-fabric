package net.caffeinemc.sodium.mixin.core.matrix;

import net.caffeinemc.sodium.interop.vanilla.math.matrix.Matrix4fExtended;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Quaternion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Matrix4f.class)
public class MixinMatrix4f implements Matrix4fExtended {
    @Shadow
    protected float a00;

    @Shadow
    protected float a01;

    @Shadow
    protected float a02;

    @Shadow
    protected float a03;

    @Shadow
    protected float a10;

    @Shadow
    protected float a11;

    @Shadow
    protected float a12;

    @Shadow
    protected float a13;

    @Shadow
    protected float a20;

    @Shadow
    protected float a21;

    @Shadow
    protected float a22;

    @Shadow
    protected float a23;

    @Shadow
    protected float a30;

    @Shadow
    protected float a31;

    @Shadow
    protected float a32;

    @Shadow
    protected float a33;

    @Override
    public float transformVecX(float x, float y, float z) {
        return (this.a00 * x) + (this.a01 * y) + (this.a02 * z) + (this.a03 * 1.0f);
    }

    @Override
    public float transformVecY(float x, float y, float z) {
        return (this.a10 * x) + (this.a11 * y) + (this.a12 * z) + (this.a13 * 1.0f);
    }

    @Override
    public float transformVecZ(float x, float y, float z) {
        return (this.a20 * x) + (this.a21 * y) + (this.a22 * z) + (this.a23 * 1.0f);
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
    public float getA00() {
        return this.a00;
    }

    @Override
    public void setA00(float a00) {
        this.a00 = a00;
    }

    @Override
    public float getA01() {
        return this.a01;
    }

    @Override
    public void setA01(float a01) {
        this.a01 = a01;
    }

    @Override
    public float getA02() {
        return this.a02;
    }

    @Override
    public void setA02(float a02) {
        this.a02 = a02;
    }

    @Override
    public float getA03() {
        return this.a03;
    }

    @Override
    public void setA03(float a03) {
        this.a03 = a03;
    }

    @Override
    public float getA10() {
        return this.a10;
    }

    @Override
    public void setA10(float a10) {
        this.a10 = a10;
    }

    @Override
    public float getA11() {
        return this.a11;
    }

    @Override
    public void setA11(float a11) {
        this.a11 = a11;
    }

    @Override
    public float getA12() {
        return this.a12;
    }

    @Override
    public void setA12(float a12) {
        this.a12 = a12;
    }

    @Override
    public float getA13() {
        return this.a13;
    }

    @Override
    public void setA13(float a13) {
        this.a13 = a13;
    }

    @Override
    public float getA20() {
        return this.a20;
    }

    @Override
    public void setA20(float a20) {
        this.a20 = a20;
    }

    @Override
    public float getA21() {
        return this.a21;
    }

    @Override
    public void setA21(float a21) {
        this.a21 = a21;
    }

    @Override
    public float getA22() {
        return this.a22;
    }

    @Override
    public void setA22(float a22) {
        this.a22 = a22;
    }

    @Override
    public float getA23() {
        return this.a23;
    }

    @Override
    public void setA23(float a23) {
        this.a23 = a23;
    }

    @Override
    public float getA30() {
        return this.a30;
    }

    @Override
    public void setA30(float a30) {
        this.a30 = a30;
    }

    @Override
    public float getA31() {
        return this.a31;
    }

    @Override
    public void setA31(float a31) {
        this.a31 = a31;
    }

    @Override
    public float getA32() {
        return this.a32;
    }

    @Override
    public void setA32(float a32) {
        this.a32 = a32;
    }

    @Override
    public float getA33() {
        return this.a33;
    }

    @Override
    public void setA33(float a33) {
        this.a33 = a33;
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

        float a01 = this.a01 * ta11 + this.a02 * ta21;
        float a02 = this.a01 * ta12 + this.a02 * ta22;
        float a11 = this.a11 * ta11 + this.a12 * ta21;
        float a12 = this.a11 * ta12 + this.a12 * ta22;
        float a21 = this.a21 * ta11 + this.a22 * ta21;
        float a22 = this.a21 * ta12 + this.a22 * ta22;
        float a31 = this.a31 * ta11 + this.a32 * ta21;
        float a32 = this.a31 * ta12 + this.a32 * ta22;

        this.a01 = a01;
        this.a02 = a02;
        this.a11 = a11;
        this.a12 = a12;
        this.a21 = a21;
        this.a22 = a22;
        this.a31 = a31;
        this.a32 = a32;
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

        float a00 = this.a00 * ta00 + this.a02 * ta20;
        float a02 = this.a00 * ta02 + this.a02 * ta22;
        float a10 = this.a10 * ta00 + this.a12 * ta20;
        float a12 = this.a10 * ta02 + this.a12 * ta22;
        float a20 = this.a20 * ta00 + this.a22 * ta20;
        float a22 = this.a20 * ta02 + this.a22 * ta22;
        float a30 = this.a30 * ta00 + this.a32 * ta20;
        float a32 = this.a30 * ta02 + this.a32 * ta22;

        this.a00 = a00;
        this.a02 = a02;
        this.a10 = a10;
        this.a12 = a12;
        this.a20 = a20;
        this.a22 = a22;
        this.a30 = a30;
        this.a32 = a32;
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

        float a00 = this.a00 * ta00 + this.a01 * ta10;
        float a01 = this.a00 * ta01 + this.a01 * ta11;
        float a10 = this.a10 * ta00 + this.a11 * ta10;
        float a11 = this.a10 * ta01 + this.a11 * ta11;
        float a20 = this.a20 * ta00 + this.a21 * ta10;
        float a21 = this.a20 * ta01 + this.a21 * ta11;
        float a30 = this.a30 * ta00 + this.a31 * ta10;
        float a31 = this.a30 * ta01 + this.a31 * ta11;

        this.a00 = a00;
        this.a01 = a01;
        this.a10 = a10;
        this.a11 = a11;
        this.a20 = a20;
        this.a21 = a21;
        this.a30 = a30;
        this.a31 = a31;
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

        float a00 = this.a00 * ta00 + this.a01 * ta10 + this.a02 * ta20;
        float a01 = this.a00 * ta01 + this.a01 * ta11 + this.a02 * ta21;
        float a02 = this.a00 * ta02 + this.a01 * ta12 + this.a02 * ta22;
        float a10 = this.a10 * ta00 + this.a11 * ta10 + this.a12 * ta20;
        float a11 = this.a10 * ta01 + this.a11 * ta11 + this.a12 * ta21;
        float a12 = this.a10 * ta02 + this.a11 * ta12 + this.a12 * ta22;
        float a20 = this.a20 * ta00 + this.a21 * ta10 + this.a22 * ta20;
        float a21 = this.a20 * ta01 + this.a21 * ta11 + this.a22 * ta21;
        float a22 = this.a20 * ta02 + this.a21 * ta12 + this.a22 * ta22;
        float a30 = this.a30 * ta00 + this.a31 * ta10 + this.a32 * ta20;
        float a31 = this.a30 * ta01 + this.a31 * ta11 + this.a32 * ta21;
        float a32 = this.a30 * ta02 + this.a31 * ta12 + this.a32 * ta22;

        this.a00 = a00;
        this.a01 = a01;
        this.a02 = a02;
        this.a10 = a10;
        this.a11 = a11;
        this.a12 = a12;
        this.a20 = a20;
        this.a21 = a21;
        this.a22 = a22;
        this.a30 = a30;
        this.a31 = a31;
        this.a32 = a32;
    }
}
