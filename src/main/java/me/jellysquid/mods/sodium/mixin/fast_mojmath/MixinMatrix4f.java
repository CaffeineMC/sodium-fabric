package me.jellysquid.mods.sodium.mixin.fast_mojmath;

import me.jellysquid.mods.sodium.client.render.matrix.ExtendedMatrix;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.util.math.Quaternion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import static me.jellysquid.mods.sodium.client.util.MathUtil.fma;

@Mixin(Matrix4f.class)
public class MixinMatrix4f implements ExtendedMatrix {

    @Shadow protected float a30;

    @Shadow protected float a31;

    @Shadow protected float a32;

    @Shadow protected float a00;

    @Shadow protected float a01;

    @Shadow protected float a02;

    @Shadow protected float a03;

    @Shadow protected float a33;

    @Shadow protected float a10;

    @Shadow protected float a11;

    @Shadow protected float a12;

    @Shadow protected float a13;

    @Shadow protected float a20;

    @Shadow protected float a21;

    @Shadow protected float a22;

    @Shadow protected float a23;

    @Override
    public void rotate(Quaternion quaternion) {
        // TODO: de-obfuscate this soup
        float float_1 = quaternion.getB();
        float float_2 = quaternion.getC();
        float float_3 = quaternion.getD();
        float float_4 = quaternion.getA();
        float float_5 = 2.0F * float_1 * float_1;
        float float_6 = 2.0F * float_2 * float_2;
        float float_7 = 2.0F * float_3 * float_3;
        float ta00 = 1.0F - float_6 - float_7;
        float ta11 = 1.0F - float_7 - float_5;
        float ta22 = 1.0F - float_5 - float_6;
        float float_8 = float_1 * float_2;
        float float_9 = float_2 * float_3;
        float float_10 = float_3 * float_1;
        float float_11 = float_1 * float_4;
        float float_12 = float_2 * float_4;
        float float_13 = float_3 * float_4;
        float ta10 = 2.0F * (float_8 + float_13);
        float ta01 = 2.0F * (float_8 - float_13);
        float ta20 = 2.0F * (float_10 - float_12);
        float ta02 = 2.0F * (float_10 + float_12);
        float ta21 = 2.0F * (float_9 + float_11);
        float ta12 = 2.0F * (float_9 - float_11);

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

    @Override
    public void translate(float x, float y, float z) {
        this.a03 = fma(this.a00, x, fma(this.a01, y, fma(this.a02, z, this.a03)));
        this.a13 = fma(this.a10, x, fma(this.a11, y, fma(this.a12, z, this.a13)));
        this.a23 = fma(this.a20, x, fma(this.a21, y, fma(this.a22, z, this.a23)));
        this.a33 = fma(this.a30, x, fma(this.a31, y, fma(this.a32, z, this.a33)));
    }

}
