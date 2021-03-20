package me.jellysquid.mods.sodium.mixin.core.matrix;

import me.jellysquid.mods.sodium.client.util.UnsafeUtil;
import me.jellysquid.mods.sodium.client.util.math.Matrix4fExtended;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Quaternion;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import sun.misc.Unsafe;

import java.nio.BufferUnderflowException;
import java.nio.FloatBuffer;

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
    public void translate(float x, float y, float z) {
        this.a03 = this.a00 * x + this.a01 * y + this.a02 * z + this.a03;
        this.a13 = this.a10 * x + this.a11 * y + this.a12 * z + this.a13;
        this.a23 = this.a20 * x + this.a21 * y + this.a22 * z + this.a23;
        this.a33 = this.a30 * x + this.a31 * y + this.a32 * z + this.a33;
    }

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

    /**
     * @reason Optimize
     * @author JellySquid
     */
    @Environment(EnvType.CLIENT)
    @Overwrite
    public void writeToBuffer(FloatBuffer buf) {
        if (buf.remaining() < 16) {
            throw new BufferUnderflowException();
        }

        if (buf.isDirect() && UnsafeUtil.isAvailable()) {
            this.writeToBufferUnsafe(buf);
        } else {
            this.writeToBufferSafe(buf);
        }
    }

    private void writeToBufferUnsafe(FloatBuffer buf) {
        long addr = MemoryUtil.memAddress(buf);

        Unsafe unsafe = UnsafeUtil.instance();
        unsafe.putFloat(addr + 0, this.a00);
        unsafe.putFloat(addr + 4, this.a10);
        unsafe.putFloat(addr + 8, this.a20);
        unsafe.putFloat(addr + 12, this.a30);
        unsafe.putFloat(addr + 16, this.a01);
        unsafe.putFloat(addr + 20, this.a11);
        unsafe.putFloat(addr + 24, this.a21);
        unsafe.putFloat(addr + 28, this.a31);
        unsafe.putFloat(addr + 32, this.a02);
        unsafe.putFloat(addr + 36, this.a12);
        unsafe.putFloat(addr + 40, this.a22);
        unsafe.putFloat(addr + 44, this.a32);
        unsafe.putFloat(addr + 48, this.a03);
        unsafe.putFloat(addr + 52, this.a13);
        unsafe.putFloat(addr + 56, this.a23);
        unsafe.putFloat(addr + 60, this.a33);
    }

    private void writeToBufferSafe(FloatBuffer buf) {
        buf.put(0, this.a00);
        buf.put(1, this.a10);
        buf.put(2, this.a20);
        buf.put(3, this.a30);
        buf.put(4, this.a01);
        buf.put(5, this.a11);
        buf.put(6, this.a21);
        buf.put(7, this.a31);
        buf.put(8, this.a02);
        buf.put(9, this.a12);
        buf.put(10, this.a22);
        buf.put(11, this.a32);
        buf.put(12, this.a03);
        buf.put(13, this.a13);
        buf.put(14, this.a23);
        buf.put(15, this.a33);
    }
}
