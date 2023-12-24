package me.jellysquid.mods.sodium.client.compatibility.checks;

import net.minecraft.util.WinNativeModuleUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Utility class for determining whether the current process has been injected into or otherwise modified. This should
 * generally only be accessed after OpenGL context creation, as most third-party software waits until the OpenGL ICD
 * is initialized before injecting.
 */
public class ModuleScanner {
    private static final Logger LOGGER = LoggerFactory.getLogger("Sodium-Win32ModuleChecks");

    private static final String[] RTSS_HOOKS_MODULE_NAMES = { "RTSSHooks64.dll", "RTSSHooks.dll" };

    public static void checkModules() {
        List<WinNativeModuleUtil.NativeModule> modules;

        try {
            modules = WinNativeModuleUtil.collectNativeModules();
        } catch (Throwable t) {
            LOGGER.warn("Failed to scan the currently loaded modules", t);
            return;
        }

        if (modules.isEmpty()) {
            // Platforms other than Windows will not return anything.
            return;
        }

        // RivaTuner hooks the wglCreateContext function, and leaves itself behind as a loaded module
        if (Configuration.WIN32_RTSS_HOOKS && isModuleLoaded(modules, RTSS_HOOKS_MODULE_NAMES)) {
            throw new RuntimeException("RivaTuner Statistics Server (RTSS) is not compatible with Sodium, " +
                    "see here for more details: https://github.com/CaffeineMC/sodium-fabric/wiki/Known-Issues#rtss-incompatible");
        }
    }

    private static boolean isModuleLoaded(List<WinNativeModuleUtil.NativeModule> modules, String[] names) {
        for (var name : names) {
            for (var module : modules) {
                if (module.path.equalsIgnoreCase(name)) {
                    return true;
                }
            }
        }

        return false;
    }
}
