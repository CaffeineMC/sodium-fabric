package me.jellysquid.mods.sodium.mixin.fast_mojmath;

import me.jellysquid.mods.sodium.client.render.matrix.ExtendedMatrix;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Quaternion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Deque;

@SuppressWarnings("ConstantConditions")
@Mixin(MatrixStack.class)
public class MixinMatrixStack {
    @Shadow
    @Final
    private Deque<MatrixStack.Entry> stack;

    /**
     * @author JellySquid
     */
    @Overwrite
    public void translate(double x, double y, double z) {
        MatrixStack.Entry entry = this.stack.getLast();

        ((ExtendedMatrix) (Object) entry.getModel()).translate((float) x, (float) y, (float) z);
    }

    /**
     * @author JellySquid
     */
    @Overwrite
    public void multiply(Quaternion q) {
        MatrixStack.Entry entry = this.stack.getLast();
        ((ExtendedMatrix) (Object) entry.getModel()).rotate(q);
        ((ExtendedMatrix) (Object) entry.getNormal()).rotate(q);
    }

}
