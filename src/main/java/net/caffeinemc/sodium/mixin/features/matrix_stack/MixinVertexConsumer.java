package net.caffeinemc.sodium.mixin.features.matrix_stack;

import net.minecraft.client.render.VertexConsumer;

import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(VertexConsumer.class)
public interface MixinVertexConsumer {
    @Shadow
    VertexConsumer normal(float x, float y, float z);

    @Shadow
    VertexConsumer vertex(double x, double y, double z);

    /**
     * @reason Avoid allocations
     * @author JellySquid
     */
    @Overwrite
    default VertexConsumer vertex(Matrix4f matrix, float x, float y, float z) {
        float x2 = Math.fma(matrix.m00(), x, Math.fma(matrix.m10(), y, Math.fma(matrix.m20(), z, matrix.m30())));
        float y2 = Math.fma(matrix.m01(), x, Math.fma(matrix.m11(), y, Math.fma(matrix.m21(), z, matrix.m31())));
        float z2 = Math.fma(matrix.m02(), x, Math.fma(matrix.m12(), y, Math.fma(matrix.m22(), z, matrix.m32())));

        return this.vertex(x2, y2, z2);
    }

    /**
     * @reason Avoid allocations
     * @author JellySquid
     */
    @Overwrite
    default VertexConsumer normal(Matrix3f matrix, float x, float y, float z) {
        float x2 = Math.fma(matrix.m00(), x, Math.fma(matrix.m10(), y, matrix.m20() * z));
        float y2 = Math.fma(matrix.m01(), x, Math.fma(matrix.m11(), y, matrix.m21() * z));
        float z2 = Math.fma(matrix.m02(), x, Math.fma(matrix.m12(), y, matrix.m22() * z));

        return this.normal(x2, y2, z2);
    }
}
