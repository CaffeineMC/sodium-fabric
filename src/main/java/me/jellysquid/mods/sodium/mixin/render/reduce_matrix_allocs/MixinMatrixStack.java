package me.jellysquid.mods.sodium.mixin.render.reduce_matrix_allocs;

import net.minecraft.client.util.math.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayDeque;
import java.util.Deque;

@Mixin(MatrixStack.class)
public class MixinMatrixStack {
    @Shadow
    @Final
    private Deque<MatrixStack.Entry> stack;

    private Deque<Matrix4f> pool4 = new ArrayDeque<>(4);
    private Deque<Matrix3f> pool3 = new ArrayDeque<>(4);

    private final Matrix4f cache = new Matrix4f();

    /**
     * @author JellySquid
     */
    @Overwrite
    public void translate(double x, double y, double z) {
        MatrixStack.Entry entry = this.stack.getLast();
        entry.getModel().multiply(translateCached((float) x, (float) y, (float) z));
    }

    @Redirect(method = "push", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/Matrix4f;copy()Lnet/minecraft/client/util/math/Matrix4f;"))
    private Matrix4f copy4(Matrix4f src) {
        Matrix4f dst = this.pool4.poll();

        if (dst == null) {
            dst = new Matrix4f(src);
        } else {
            Sodium_Matrix4fUtil.copy(src, dst);
        }

        return dst;
    }

    @Redirect(method = "push", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/Matrix3f;copy()Lnet/minecraft/client/util/math/Matrix3f;"))
    private Matrix3f copy3(Matrix3f src) {
        Matrix3f dst = this.pool3.poll();

        if (dst == null) {
            dst = new Matrix3f(src);
        } else {
            Sodium_Matrix3fUtil.copy(src, dst);
        }

        return dst;
    }

    /**
     * @author JellySquid
     */
    @Overwrite
    public void pop() {
        MatrixStack.Entry entry = this.stack.removeLast();

        this.pool4.add(entry.getModel());
        this.pool3.add(entry.getNormal());
    }

    private Matrix4f translateCached(float x, float y, float z) {
        Sodium_Matrix4fUtil.setTranslation(this.cache, x, y, z);

        return this.cache;
    }
}
