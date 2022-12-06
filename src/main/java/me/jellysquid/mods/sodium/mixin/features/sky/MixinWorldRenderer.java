package me.jellysquid.mods.sodium.mixin.features.sky;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.CameraSubmersionType;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.MathHelper;

import net.minecraft.util.math.Vec3d;

@Mixin(WorldRenderer.class)
public class MixinWorldRenderer {
    @Shadow
    @Final
    private MinecraftClient client;

    /**
     * <p>Prevents the sky layer from rendering when the fog distance is reduced
     * from the default. This helps prevent situations where the sky can be seen
     * through chunks culled by fog occlusion. This also fixes the vanilla issue
     * <a href="https://bugs.mojang.com/browse/MC-152504">MC-152504</a> since it
     * is also caused by being able to see the sky through invisible chunks.</p>
     * 
     * <p>However, this fix comes with some caveats. When underwater, it becomes 
     * impossible to see the sun, stars, and moon since the sky is not rendered.
     * While this does not exactly match the vanilla game, it is consistent with
     * what Bedrock Edition does, so it can be considered vanilla-style. This is
     * also more "correct" in the sense that underwater fog is applied to chunks
     * outside of water, so the fog should also be covering the sun and sky.</p>
     * 
     * <p>When updating Sodium to new releases of the game, please check for new
     * ways the fog can be reduced in {@link BackgroundRenderer#applyFog(Camera, BackgroundRenderer.FogType, float, boolean, float)} ()}.</p>
     */
    @Inject(method = "renderSky(Lnet/minecraft/client/util/math/MatrixStack;Lorg/joml/Matrix4f;FLnet/minecraft/client/render/Camera;ZLjava/lang/Runnable;)V", at = @At("HEAD"), cancellable = true)
    private void preRenderSky(MatrixStack matrices, Matrix4f projectionMatrix, float tickDelta, Camera camera, boolean bl, Runnable runnable, CallbackInfo ci) {
        // Cancels sky rendering when the camera is submersed underwater.
        // This prevents the sky from being visible through chunks culled by Sodium's fog occlusion.
        // Fixes https://bugs.mojang.com/browse/MC-152504.
        // Credit to bytzo for noticing the change in 1.18.2.
        if (camera.getSubmersionType() == CameraSubmersionType.WATER) {
            ci.cancel();
        }
    }
}
