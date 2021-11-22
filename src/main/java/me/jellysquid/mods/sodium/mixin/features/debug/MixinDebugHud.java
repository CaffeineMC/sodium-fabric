package me.jellysquid.mods.sodium.mixin.features.debug;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import net.minecraft.client.gui.hud.DebugHud;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.management.ManagementFactory;
import java.util.List;

@Mixin(DebugHud.class)
public abstract class MixinDebugHud {
    @Shadow
    private static long toMiB(long bytes) {
        throw new UnsupportedOperationException();
    }

    @Inject(method = "getRightText", at = @At("RETURN"), cancellable = true)
    private void injectRightTextReturn(CallbackInfoReturnable<List<String>> cir) {
        List<String> strings = cir.getReturnValue();
        strings.add(3, getNativeMemoryString());
        strings.add(10, "");
        strings.add(11, "Sodium Renderer");
        strings.add(12, Formatting.UNDERLINE + getFormattedVersionText());

        var renderer = SodiumWorldRenderer.instanceNullable();

        if (renderer != null) {
            strings.addAll(13, renderer.getMemoryDebugStrings());
        }

        cir.setReturnValue(strings);
    }

    private static String getFormattedVersionText() {
        String version = SodiumClientMod.getVersion();
        Formatting color;

        if (version.endsWith("-dirty")) {
            color = Formatting.RED;
        } else if (version.contains("+rev.")) {
            color = Formatting.LIGHT_PURPLE;
        } else {
            color = Formatting.GREEN;
        }

        return color + version;
    }

    private static String getNativeMemoryString() {
        return "Off-Heap: +" + toMiB(getNativeMemoryUsage()) + "MB";
    }

    private static long getNativeMemoryUsage() {
        return ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed() + NativeBuffer.getTotalAllocated();
    }
}
