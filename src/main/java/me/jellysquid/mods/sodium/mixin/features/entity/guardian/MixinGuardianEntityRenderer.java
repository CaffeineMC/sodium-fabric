package me.jellysquid.mods.sodium.mixin.features.entity.guardian;

import net.minecraft.client.render.entity.GuardianEntityRenderer;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GuardianEntityRenderer.class)
public class MixinGuardianEntityRenderer {
    /**
     * @reason Use the time of day instead of the time of the world in guardian beam rendering
     * @author AMereBagatelle
     */
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getTime()J"))
    private long useTimeOfDay(World world) {
        return world.getTimeOfDay();
    }
}
