package me.jellysquid.mods.sodium.mixin.features.matrix_stack;

import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;
import java.util.Deque;

@Mixin(MatrixStack.class)
public abstract class MixinMatrixStack {
    @Shadow
    @Final
    private Deque<MatrixStack.Entry> stack;

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
