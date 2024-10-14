package net.caffeinemc.mods.sodium.mixin.features.render.world.clouds;

import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.vertex.PoseStack;
import net.caffeinemc.mods.sodium.client.render.immediate.CloudRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Shadow
    private @Nullable ClientLevel level;
    @Shadow
    private int ticks;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Unique
    private CloudRenderer cloudRenderer;

    /**
     * @author jellysquid3
     * @reason Optimize cloud rendering
     */
    @Group(name = "sodium$cloudsOverride", min = 1, max = 1)
    @Dynamic
    @Inject(method = "method_62205", at = @At(value = "HEAD"), cancellable = true, require = 0)
    public void renderCloudsFabric(ResourceHandle<?> resourceHandle, int color, CloudStatus cloudStatus, float f, Matrix4f modelView, Matrix4f projectionMatrix, Vec3 vec3, float tickDelta, CallbackInfo ci) {
        ci.cancel();

        sodium$renderClouds(modelView, projectionMatrix, color);
    }

    @Group(name = "sodium$cloudsOverride", min = 1, max = 1)
    @Dynamic
    @Inject(method = { "lambda$addCloudsPass$6" }, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/CloudRenderer;render(ILnet/minecraft/client/CloudStatus;FLorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lnet/minecraft/world/phys/Vec3;F)V"), cancellable = true, require = 0) // Inject after Forge checks dimension support
    public void renderCloudsNeo(ResourceHandle<?> resourcehandle, float p_365209_, Vec3 p_362985_, Matrix4f modelView, Matrix4f projectionMatrix, int color, CloudStatus p_364196_, float p_362337_, CallbackInfo ci) {
        ci.cancel();

        sodium$renderClouds(modelView, projectionMatrix, color);
    }

    private void sodium$renderClouds(Matrix4f modelView, Matrix4f projectionMatrix, int color) {
        if (this.cloudRenderer == null) {
            this.cloudRenderer = new CloudRenderer(this.minecraft.getResourceManager());
        }

        ClientLevel level = Objects.requireNonNull(this.level);
        Camera camera = this.minecraft.gameRenderer.getMainCamera();

        this.cloudRenderer.render(camera, level, projectionMatrix, modelView, this.ticks, this.minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false), color);
    }

    @Inject(method = "onResourceManagerReload(Lnet/minecraft/server/packs/resources/ResourceManager;)V", at = @At("RETURN"))
    private void onReload(ResourceManager manager, CallbackInfo ci) {
        if (this.cloudRenderer != null) {
            this.cloudRenderer.reloadTextures(manager);
        }
    }

    @Inject(method = "allChanged()V", at = @At("RETURN"))
    private void onReload(CallbackInfo ci) {
        // will be re-allocated on next use
        if (this.cloudRenderer != null) {
            this.cloudRenderer.destroy();
            this.cloudRenderer = null;
        }
    }

    @Inject(method = "close", at = @At("RETURN"))
    private void onClose(CallbackInfo ci) {
        // will never be re-allocated, as the renderer is shutting down
        if (this.cloudRenderer != null) {
            this.cloudRenderer.destroy();
            this.cloudRenderer = null;
        }
    }
}
