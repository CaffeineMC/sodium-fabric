package me.jellysquid.mods.sodium.mixin.features.render.immediate.matrix_stack;

import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.*;

import java.util.ArrayDeque;
import java.util.Deque;

@Mixin(MatrixStack.class)
public abstract class MatrixStackMixin {
    @Shadow
    @Final
    private Deque<MatrixStack.Entry> stack;

    @Unique
    private final Deque<MatrixStack.Entry> cache = new ArrayDeque<>();


    /**
     * @author JellySquid
     * @reason Re-use entries when possible
     */
    @Overwrite
    public void push() {
        var prev = this.stack.getLast();

        MatrixStack.Entry entry;

        if (!this.cache.isEmpty()) {
            entry = this.cache.removeLast();
            entry.getPositionMatrix()
                    .set(prev.getPositionMatrix());
            entry.getNormalMatrix()
                    .set(prev.getNormalMatrix());
        } else {
            entry = new MatrixStack.Entry(new Matrix4f(prev.getPositionMatrix()), new Matrix3f(prev.getNormalMatrix()));
        }

        entry.canSkipNormalization = prev.canSkipNormalization;

        this.stack.addLast(entry);
    }

    /**
     * @author JellySquid
     * @reason Re-use entries when possible
     */
    @Overwrite
    public void pop() {
        this.cache.addLast(this.stack.removeLast());
    }
}
