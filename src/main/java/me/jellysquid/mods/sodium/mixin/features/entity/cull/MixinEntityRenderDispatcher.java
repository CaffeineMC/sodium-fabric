package me.jellysquid.mods.sodium.mixin.features.entity.cull;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.entity.EntityLabelAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;

@Mixin(EntityRenderDispatcher.class)
public abstract class MixinEntityRenderDispatcher {
    @Shadow
    protected abstract int getLight(Entity entity, float tickDelta);
    
    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/entity/EntityRenderer;render(Lnet/minecraft/entity/Entity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"
        ),
        locals = LocalCapture.CAPTURE_FAILHARD,
        cancellable = true
    )
    private <E extends Entity> void preRenderEntity(Entity entity, double x, double y, double z, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo callbackInfo, EntityRenderer<? super E> entityRenderer) {
        // If the entity about to be rendered has a nametag, has no outline, and
        // is also not visible, just render the entity nametag and nothing else.
        if (((EntityLabelAccessor) entityRenderer).bridge$hasLabel(entity) && !MinecraftClient.getInstance().hasOutline(entity) && !SodiumWorldRenderer.getInstance().isEntityVisible(entity)) {
            ((EntityLabelAccessor) entityRenderer).bridge$renderLabelIfPresent(entity, entity.getDisplayName(), matrices, vertexConsumers, this.getLight(entity, tickDelta));

            matrices.pop();
            callbackInfo.cancel();
        }
    }

    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/entity/EntityRenderer;render(Lnet/minecraft/entity/Entity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            shift = Shift.AFTER
        ),
        cancellable = true
    )
    private void postRenderEntity(Entity entity, double x, double y, double z, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo callbackInfo) {
        // If the entity that was rendered had an outline and was not visible,
        // cancel the rest of the entity rendering.
        if (MinecraftClient.getInstance().hasOutline(entity) && !SodiumWorldRenderer.getInstance().isEntityVisible(entity)) {
            matrices.pop();
            callbackInfo.cancel();
        }
    }
}
