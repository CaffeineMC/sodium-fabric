package me.jellysquid.mods.sodium.mixin.features.render.world.clouds;

import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BackgroundRenderer.class)
public interface BackgroundRendererInvoker {
    @Invoker
    static BackgroundRenderer.StatusEffectFogModifier invokeGetFogModifier(Entity entity, float tickDelta) {
        throw new AssertionError();
    }
}
