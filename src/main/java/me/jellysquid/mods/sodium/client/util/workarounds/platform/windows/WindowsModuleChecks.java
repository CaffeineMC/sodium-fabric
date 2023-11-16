package me.jellysquid.mods.sodium.client.util.workarounds.platform.windows;

import net.minecraft.util.Util;
import net.minecraft.util.WinNativeModuleUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class WindowsModuleChecks {
    private static final Logger LOGGER = LoggerFactory.getLogger("Sodium-Win32ModuleChecks");

    public static void checkModules() {
        if (Util.getOperatingSystem() != Util.OperatingSystem.WINDOWS) {
            return;
        }

        LOGGER.warn("Checking for problematic loaded Win32 modules... (this may take a moment)");

        List<WinNativeModuleUtil.NativeModule> modules;

        try {
            modules = WinNativeModuleUtil.collectNativeModules();
        } catch (Throwable t) {
            LOGGER.warn("Failed to scan the currently loaded modules", t);
            return;
        }

        // RivaTuner hooks the wglCreateContext function, and leaves itself behind as a loaded module
        if (Boolean.parseBoolean(System.getProperty("sodium.checks.win32.rtss", "true")) && modules.stream().anyMatch(module -> module.path.equalsIgnoreCase("RTSSHooks64.dll"))) {
            LOGGER.error("------------------------------------------------------------------------------------------------------------");
            LOGGER.error("READ ME! You appear to be using the RivaTuner Statistics Server (RTSS)!");
            LOGGER.error("  * Rivatuner will cause extreme performance issues when using Sodium, and it will likely fill up your hard drive");
            LOGGER.error("    with error logs.");
            LOGGER.error("  * You must fully disable (or uninstall) the RivaTuner Statistics Server.");
            LOGGER.error("    * If you don't remember installing RivaTuner, check to see if you have MSI Afterburner installed.");
            LOGGER.error("  * For more information on possible workarounds and alternatives to Rivatuner, see the following issue on GitHub:");
            LOGGER.error("    https://github.com/CaffeineMC/sodium-fabric/issues/2048");
            LOGGER.error("  * HINT: If you believe this is an error, then you can force the game to start anyways by adding the " +
                "following JVM argument.");
            LOGGER.error("      -Dsodium.checks.win32.rtss" + "=false");
            LOGGER.error("  * NOTE: We will not provide support for any issues caused by using this option. You are on your own!");
            LOGGER.error("------------------------------------------------------------------------------------------------------------");

            throw new RuntimeException("RivaTuner Statistics Server (RTSS) is not compatible with Sodium, " +
                    "see this issue for more details: https://github.com/CaffeineMC/sodium-fabric/issues/2048");
        }
    }
}
