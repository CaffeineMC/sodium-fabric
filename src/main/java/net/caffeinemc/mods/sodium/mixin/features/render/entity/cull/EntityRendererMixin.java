package net.caffeinemc.mods.sodium.mixin.features.render.entity.cull;

import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {
    @Redirect(method = "shouldRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/culling/Frustum;isVisible(Lnet/minecraft/world/phys/AABB;)Z", ordinal = 0))
    private boolean preShouldRender(Frustum frustum, AABB aABB, T entity) {
        var renderer = SodiumWorldRenderer.instanceNullable();

        if (renderer == null) {
            return frustum.isVisible(aABB);
        }

        return frustum.isVisible(aABB) && renderer.isEntityVisible(entity);
    }
}
