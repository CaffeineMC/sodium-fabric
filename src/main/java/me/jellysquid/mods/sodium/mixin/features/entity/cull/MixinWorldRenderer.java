package me.jellysquid.mods.sodium.mixin.features.entity.cull;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;

@Mixin(WorldRenderer.class)
public class MixinWorldRenderer {
    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    @Final
    private EntityRenderDispatcher entityRenderDispatcher;

    @Inject(
        method = "renderEntity",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/entity/EntityRenderDispatcher;render(Lnet/minecraft/entity/Entity;DDDFFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"
        ),
        locals = LocalCapture.CAPTURE_FAILHARD,
        cancellable = true
    )
    private void stopRenderEntity(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo callbackInfo, double d, double e, double f) {
        // If the entity about to be rendered has a nametag, has no outline, and
        // is also not visible, just render the entity nametag and nothing else.
        if (entityRenderDispatcher.getRenderer(entity).hasLabel(entity) && !this.client.hasOutline(entity) && !SodiumWorldRenderer.getInstance().isEntityVisible(entity)) {
            matrices.push();
            matrices.translate(d - cameraX, e - cameraY, f - cameraZ);
            entityRenderDispatcher.getRenderer(entity).renderLabelIfPresent(entity, entity.getDisplayName(), matrices, vertexConsumers, entityRenderDispatcher.getLight(entity, tickDelta));;
            matrices.pop();

            callbackInfo.cancel();
        }
    }
}
