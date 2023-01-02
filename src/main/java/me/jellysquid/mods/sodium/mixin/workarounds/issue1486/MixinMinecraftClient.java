package me.jellysquid.mods.sodium.mixin.workarounds.issue1486;

import me.jellysquid.mods.sodium.client.util.workarounds.DriverWorkarounds;
import net.minecraft.GameVersion;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClient {
    @Redirect(method = "getWindowTitle", at = @At(value = "INVOKE", target = "Lnet/minecraft/GameVersion;getName()Ljava/lang/String;"))
    private String overrideWindowTitle(GameVersion instance) {
        if (DriverWorkarounds.isWorkaroundEnabled(DriverWorkarounds.Reference.ISSUE_1486)) {
            return "(version hidden from driver)";
        }
        return instance.getName();
    }
}
