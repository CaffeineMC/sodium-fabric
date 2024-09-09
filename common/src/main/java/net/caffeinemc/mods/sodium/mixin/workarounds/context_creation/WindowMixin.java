package net.caffeinemc.mods.sodium.mixin.workarounds.context_creation;

import com.mojang.blaze3d.platform.DisplayData;
import com.mojang.blaze3d.platform.ScreenManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.WindowEventHandler;
import net.caffeinemc.mods.sodium.client.compatibility.checks.PostLaunchChecks;
import net.caffeinemc.mods.sodium.client.compatibility.checks.ModuleScanner;
import net.caffeinemc.mods.sodium.client.compatibility.environment.GLContextInfo;
import net.minecraft.Util;
import org.lwjgl.opengl.WGL;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public class WindowMixin {
    @Shadow
    @Final
    private static Logger LOGGER;

    @Shadow
    @Final
    private long window;

    @Unique
    private long wglPrevContext = MemoryUtil.NULL;

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL;createCapabilities()Lorg/lwjgl/opengl/GLCapabilities;", shift = At.Shift.AFTER, remap = false))
    private void postContextReady(WindowEventHandler eventHandler, ScreenManager monitorTracker, DisplayData settings, String videoMode, String title, CallbackInfo ci) {
        GLContextInfo driver = GLContextInfo.create();

        if (driver == null) {
            LOGGER.warn("Could not retrieve identifying strings for OpenGL implementation");
        } else {
            LOGGER.info("OpenGL Vendor: {}", driver.vendor());
            LOGGER.info("OpenGL Renderer: {}", driver.renderer());
            LOGGER.info("OpenGL Version: {}", driver.version());
        }

        // Capture the current WGL context so that we can detect it being replaced later.
        if (Util.getPlatform() == Util.OS.WINDOWS) {
            this.wglPrevContext = WGL.wglGetCurrentContext();
        } else {
            this.wglPrevContext = MemoryUtil.NULL;
        }

        PostLaunchChecks.onContextInitialized();
        ModuleScanner.checkModules(this.window);
    }

    @Inject(method = "updateDisplay", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;flipFrame(J)V", shift = At.Shift.AFTER, remap = false))
    private void preSwapBuffers(CallbackInfo ci) {
        if (this.wglPrevContext == MemoryUtil.NULL) {
            // There is no prior recorded context.
            return;
        }

        var context = WGL.wglGetCurrentContext();

        if (this.wglPrevContext == context) {
            // The context has not changed.
            return;
        }

        // Something has decided to replace the OpenGL context, which is not a good sign
        LOGGER.warn("The OpenGL context appears to have been suddenly replaced! Something has likely just injected into the game process.");

        // Likely, this indicates a module was injected into the current process. We should check that
        // nothing problematic was just installed.
        ModuleScanner.checkModules(this.window);

        // If we didn't find anything problematic (which would have thrown an exception), then let's just record
        // the new context pointer and carry on.
        this.wglPrevContext = context;
    }
}
