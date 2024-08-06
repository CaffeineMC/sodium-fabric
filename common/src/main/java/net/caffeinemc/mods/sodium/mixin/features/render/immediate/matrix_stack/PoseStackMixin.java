package net.caffeinemc.mods.sodium.mixin.features.render.immediate.matrix_stack;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.*;
import com.mojang.blaze3d.vertex.PoseStack;

import java.util.ArrayDeque;
import java.util.Deque;

@Mixin(PoseStack.class)
public abstract class PoseStackMixin {
    @Shadow
    @Final
    private Deque<PoseStack.Pose> poseStack;

    @Unique
    private final Deque<PoseStack.Pose> cache = new ArrayDeque<>();


    /**
     * @author JellySquid
     * @reason Re-use entries when possible
     */
    @Overwrite
    public void pushPose() {
        var prev = this.poseStack.getLast();

        PoseStack.Pose entry;

        if (!this.cache.isEmpty()) {
            entry = this.cache.removeLast();
            entry.pose()
                    .set(prev.pose());
            entry.normal()
                    .set(prev.normal());
        } else {
            entry = new PoseStack.Pose(new Matrix4f(prev.pose()), new Matrix3f(prev.normal()));
        }

        entry.trustedNormals = prev.trustedNormals;

        this.poseStack.addLast(entry);
    }

    /**
     * @author JellySquid
     * @reason Re-use entries when possible
     */
    @Overwrite
    public void popPose() {
        this.cache.addLast(this.poseStack.removeLast());
    }
}
