package net.caffeinemc.mods.sodium.neoforge.mixin.workarounds.context_creation;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.Window;
import net.caffeinemc.mods.sodium.client.compatibility.workarounds.Workarounds;
import net.caffeinemc.mods.sodium.client.compatibility.workarounds.nvidia.NvidiaWorkarounds;
import net.caffeinemc.mods.sodium.client.services.PlatformRuntimeInformation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

@Mixin(Window.class)
public class WindowMixin {
    @WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/neoforged/fml/loading/ImmediateWindowHandler;setupMinecraftWindow(Ljava/util/function/IntSupplier;Ljava/util/function/IntSupplier;Ljava/util/function/Supplier;Ljava/util/function/LongSupplier;)J"), remap = false)
    private long wrapGlfwCreateWindowForge(final IntSupplier width, final IntSupplier height, final Supplier<String> title, final LongSupplier monitor, Operation<Long> op) {
        final boolean applyNvidiaWorkarounds = Workarounds.isWorkaroundEnabled(Workarounds.Reference.NVIDIA_THREADED_OPTIMIZATIONS);

        if (applyNvidiaWorkarounds && !PlatformRuntimeInformation.getInstance().platformHasEarlyLoadingScreen()) {
            NvidiaWorkarounds.install();
        }

        try {
            return op.call(width, height, title, monitor);
        } finally {
            if (applyNvidiaWorkarounds) {
                NvidiaWorkarounds.uninstall();
            }
        }
    }
}
