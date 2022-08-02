package net.caffeinemc.sodium.mixin.features.matrix_stack;

import net.caffeinemc.sodium.interop.vanilla.math.matrix.Matrix3fUtil;
import net.caffeinemc.sodium.interop.vanilla.math.matrix.Matrix4fUtil;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;
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
        return this.vertex(Matrix4fUtil.transformVectorX(matrix, x, y, z), Matrix4fUtil.transformVectorY(matrix, x, y, z), Matrix4fUtil.transformVectorZ(matrix, x, y, z));
    }

    /**
     * @reason Avoid allocations
     * @author JellySquid
     */
    @Overwrite
    default VertexConsumer normal(Matrix3f matrix, float x, float y, float z) {
        return this.normal(Matrix3fUtil.transformVectorX(matrix, x, y, z), Matrix3fUtil.transformVectorY(matrix, x, y, z), Matrix3fUtil.transformVectorZ(matrix, x, y, z));
    }
}
