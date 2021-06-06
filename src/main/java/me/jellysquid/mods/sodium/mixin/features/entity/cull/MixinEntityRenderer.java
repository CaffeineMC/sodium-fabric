package me.jellysquid.mods.sodium.mixin.features.entity.cull;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.entity.EntityLabelAccessor;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer implements EntityLabelAccessor {
    @Shadow
    protected abstract boolean hasLabel(Entity entity);

    @Shadow
    protected abstract void renderLabelIfPresent(Entity entity, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light);

    @Inject(method = "shouldRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Frustum;isVisible(Lnet/minecraft/util/math/Box;)Z", shift = At.Shift.AFTER), cancellable = true)
    private void preShouldRender(Entity entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        // If the entity isn't culled already by other means, try to perform a second pass
        if (cir.getReturnValue() && SodiumWorldRenderer.getInstance().shouldCullEntity(entity)) {
            cir.setReturnValue(false);
        }
    }

    @Override
    public boolean bridge$hasLabel(Entity entity) {
        return this.hasLabel(entity);
    }

    @Override
    public void bridge$renderLabelIfPresent(Entity entity, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        this.renderLabelIfPresent(entity, text, matrices, vertexConsumers, light);
    }
}
