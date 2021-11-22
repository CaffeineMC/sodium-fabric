package me.jellysquid.mods.sodium.mixin.core;

import me.jellysquid.mods.sodium.client.util.frustum.FrustumAccessor;
import net.minecraft.client.render.Frustum;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.Validate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Frustum.class)
public class MixinFrustum implements FrustumAccessor {
    @Shadow
    private double x;

    @Shadow
    private double y;

    @Shadow
    private double z;

    private Matrix4f projectionMatrix;
    private Matrix4f modelViewMatrix;

    @Inject(method = "init", at = @At("RETURN"))
    public void init(Matrix4f modelViewMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        this.projectionMatrix = projectionMatrix;
        this.modelViewMatrix = modelViewMatrix;
    }

    @Override
    public Matrix4f getProjectionMatrix() {
        return Validate.notNull(this.projectionMatrix);
    }

    @Override
    public Matrix4f getModelViewMatrix() {
        return Validate.notNull(this.modelViewMatrix);
    }

    @Override
    public Vec3d getPosition() {
        return new Vec3d(this.x, this.y, this.z);
    }
}
