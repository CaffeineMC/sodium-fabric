package me.jellysquid.mods.sodium.mixin.options;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WorldRenderer.class)
public class MixinWorldRenderer {
    @Redirect(method = "renderWeather", at = @At(value = "FIELD", target = "Lnet/minecraft/client/options/GameOptions;fancyGraphics:Z"))
    private boolean redirectGetFancyWeather(GameOptions options) {
        return SodiumClientMod.options().quality.weatherQuality.isFancy(options.fancyGraphics);
    }
}
