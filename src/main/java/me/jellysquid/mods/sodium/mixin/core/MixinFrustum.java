package me.jellysquid.mods.sodium.mixin.core;

import me.jellysquid.mods.sodium.interop.vanilla.math.frustum.FrustumAdapter;
import me.jellysquid.mods.sodium.interop.vanilla.math.frustum.JomlFrustum;
import net.minecraft.client.renderer.culling.Frustum;
import me.jellysquid.mods.sodium.interop.vanilla.math.JomlHelper;
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
    private double camX;

    @Shadow
    private double camY;

    @Shadow
    private double camZ;

    private Matrix4f projectionMatrix;
    private Matrix4f modelViewMatrix;

    @Inject(method = "calculateFrustum", at = @At("RETURN"))
    public void init(com.mojang.math.Matrix4f modelViewMatrix,
                     com.mojang.math.Matrix4f projectionMatrix,
                     CallbackInfo ci) {
        this.projectionMatrix = JomlHelper.copy(projectionMatrix);
        this.modelViewMatrix = JomlHelper.copy(modelViewMatrix);
    }

    @Override
    public me.jellysquid.mods.sodium.interop.vanilla.math.frustum.Frustum sodium$createFrustum() {
        Matrix4f matrix = new Matrix4f();
        matrix.set(Validate.notNull(this.projectionMatrix));
        matrix.mul(Validate.notNull(this.modelViewMatrix));

        return new JomlFrustum(matrix, new Vector3f((float) this.camX, (float) this.camY, (float) this.camZ));
    }
}
