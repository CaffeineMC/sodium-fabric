package me.jellysquid.mods.sodium.mixin.features.entity.instancing;

import me.jellysquid.mods.sodium.SodiumClient;
import me.jellysquid.mods.sodium.SodiumRender;
import me.jellysquid.mods.sodium.render.entity.BakedModelUtils;
import me.jellysquid.mods.sodium.render.entity.renderer.InstancedEntityRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class MixinWorldRenderer {

    @Shadow private ClientWorld world;

    // TODO: this is placed here because it's supposed to render before the rest of the entity stuff, but the subsequent calls to getRenderLayer cause them to flush early, so only one or two calls happen after this.
    // maybe merge a batching solution in?
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;drawCurrentLayer()V", shift = At.Shift.BEFORE))
    private void renderQueues(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, CallbackInfo ci) {
        this.world.getProfiler().push("renderInstances");
        if (SodiumClient.options().performance.useModelInstancing && InstancedEntityRenderer.isSupported(SodiumRender.DEVICE)) {
            BakedModelUtils.getInstancedRenderDispatcher().render();
        }
        this.world.getProfiler().pop();
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void closeInstancingResources(CallbackInfo ci) {
        BakedModelUtils.close();
    }
}
