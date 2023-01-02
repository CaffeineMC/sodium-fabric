package me.jellysquid.mods.sodium.mixin.workarounds.issue1486;

import me.jellysquid.mods.sodium.client.util.workarounds.DriverWorkarounds;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClient {
    @Inject(method = "getWindowTitle", at = @At("HEAD"), cancellable = true)
    private void overrideWindowTitle(CallbackInfoReturnable<String> ci) {
        if (DriverWorkarounds.isWorkaroundEnabled(DriverWorkarounds.Reference.ISSUE_1486)) {
            ci.setReturnValue("Minecraft* (version hidden from driver)");
        }
    }
}
