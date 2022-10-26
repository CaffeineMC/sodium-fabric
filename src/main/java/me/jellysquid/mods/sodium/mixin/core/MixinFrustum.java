package me.jellysquid.mods.sodium.mixin.core;

import me.jellysquid.mods.sodium.client.util.frustum.FrustumAdapter;
import me.jellysquid.mods.sodium.client.util.frustum.JomlFrustum;
import net.minecraft.client.render.Frustum;
import org.apache.commons.lang3.Validate;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Frustum.class)
public class MixinFrustum implements FrustumAdapter {
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
        this.projectionMatrix = new Matrix4f(projectionMatrix);
        this.modelViewMatrix = new Matrix4f(modelViewMatrix);
    }

    @Override
    public me.jellysquid.mods.sodium.client.util.frustum.Frustum sodium$createFrustum() {
        Matrix4f matrix = new Matrix4f();
        matrix.set(Validate.notNull(this.projectionMatrix));
        matrix.mul(Validate.notNull(this.modelViewMatrix));

        return new JomlFrustum(matrix, new Vector3f((float) this.x, (float) this.y, (float) this.z));
    }
}
