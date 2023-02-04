package me.jellysquid.mods.sodium.mixin.features.matrix_stack;

import me.jellysquid.mods.sodium.common.util.MatrixHelper;
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
        float xt = MatrixHelper.transformPositionX(matrix, x, y, z);
        float yt = MatrixHelper.transformPositionY(matrix, x, y, z);
        float zt = MatrixHelper.transformPositionZ(matrix, x, y, z);

        return this.vertex(xt, yt, zt);
    }

    /**
     * @reason Avoid allocations
     * @author JellySquid
     */
    @Overwrite
    default VertexConsumer normal(Matrix3f matrix, float x, float y, float z) {
        float xt = MatrixHelper.transformNormalX(matrix, x, y, z);
        float yt = MatrixHelper.transformNormalY(matrix, x, y, z);
        float zt = MatrixHelper.transformNormalZ(matrix, x, y, z);

        return this.normal(xt, yt, zt);
    }
}
