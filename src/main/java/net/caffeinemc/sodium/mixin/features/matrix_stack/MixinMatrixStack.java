package net.caffeinemc.sodium.mixin.features.matrix_stack;

import net.caffeinemc.sodium.interop.vanilla.math.matrix.Matrix3fExtended;
import net.caffeinemc.sodium.interop.vanilla.math.matrix.Matrix4fExtended;
import net.caffeinemc.sodium.interop.vanilla.math.matrix.MatrixUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Quaternion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Deque;

@Mixin(MatrixStack.class)
public class MixinMatrixStack {
    @Shadow
    @Final
    private Deque<MatrixStack.Entry> stack;

    /**
     * @reason Use our faster specialized function
     * @author JellySquid
     */
    @Overwrite
    public void multiply(Quaternion q) {
        MatrixStack.Entry entry = this.stack.getLast();

        Matrix4fExtended mat4 = MatrixUtil.getExtendedMatrix(entry.getPositionMatrix());
        mat4.rotate(q);

        Matrix3fExtended mat3 = MatrixUtil.getExtendedMatrix(entry.getNormalMatrix());
        mat3.rotate(q);
    }
}
