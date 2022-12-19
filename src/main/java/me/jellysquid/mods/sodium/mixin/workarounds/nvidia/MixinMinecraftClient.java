package me.jellysquid.mods.sodium.mixin.workarounds.nvidia;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClient {
    @Inject(method = "getWindowTitle", at = @At("HEAD"), cancellable = true)
    private void overrideWindowTitle(CallbackInfoReturnable<String> ci) {
        if (SodiumClientMod.options().workarounds.issue1486_hideWindowTitleToEvadeNvidiaDrivers) {
            ci.setReturnValue("Minecraft* (version hidden from driver)");
        }
    }
}
