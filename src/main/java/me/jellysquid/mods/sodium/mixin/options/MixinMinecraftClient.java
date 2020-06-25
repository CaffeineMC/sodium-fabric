package me.jellysquid.mods.sodium.mixin.options;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClient {
    /**
     * @author JellySquid
     */
    @Overwrite
    public static boolean isAmbientOcclusionEnabled() {
        return SodiumClientMod.options().quality.smoothLighting != SodiumGameOptions.LightingQuality.OFF;
    }

    @Inject(method = "onResolutionChanged", at = @At(value = "HEAD"))
    public void preResolutionChanged(CallbackInfo _) {
        float maxGuiScale = MinecraftClient.getInstance().getWindow().calculateScaleFactor(0, MinecraftClient.getInstance().forcesUnicodeFont());
        float scalePercent = (float) SodiumClientMod.options().general.guiScalePercentage / 100;
        int guiScale = (int) Math.ceil(scalePercent * maxGuiScale);

        //Prevent the game auto scaling the GUI when the scale is 0
        if (guiScale == 0) {
            guiScale = 1;
        }

        MinecraftClient.getInstance().options.guiScale = guiScale;
    }
}
