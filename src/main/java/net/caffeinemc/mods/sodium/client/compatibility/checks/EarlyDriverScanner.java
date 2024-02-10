package net.caffeinemc.mods.sodium.client.compatibility.checks;

import net.caffeinemc.mods.sodium.client.platform.MessageBox;
import net.caffeinemc.mods.sodium.client.platform.windows.WindowsDisplayDriverVersion;
import net.minecraft.Util;
import net.caffeinemc.mods.sodium.client.compatibility.environment.probe.GraphicsAdapterProbe;
import net.caffeinemc.mods.sodium.client.compatibility.environment.probe.GraphicsAdapterVendor;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Performs OpenGL driver validation before the game creates an OpenGL context. This runs during the earliest possible
 * opportunity at game startup, and uses a custom hardware probe to search for problematic drivers.
 */
public class EarlyDriverScanner {
    private static final Logger LOGGER = LoggerFactory.getLogger("Sodium-EarlyDriverScanner");

    private static final String CONSOLE_MESSAGE_TEMPLATE = """ 
            ###ERROR_DESCRIPTION###
            
            For more information, please see: ###HELP_URL###
            """;

    private static final String INTEL_GEN7_DRIVER_MESSAGE = """
            The game failed to start because the currently installed Intel Graphics Driver is not compatible.
                                        
            Installed version: ###CURRENT_DRIVER###
            Required version: 15.33.53.5161 (or newer)
                                        
            You must update your graphics card driver in order to continue.""";

    private static final String INTEL_GEN7_DRIVER_HELP_URL = "https://github.com/CaffeineMC/sodium-fabric/wiki/Driver-Compatibility#windows-intel-gen7";

    /**
     * Scans the detected graphics adapters to determine if a known broken adapter or driver is being used. If one is
     * found, then this function aborts the process with a helpful error message.
     */
    public static void scanDrivers() {
        if (Configuration.WIN32_DRIVER_INTEL_GEN7) {
            var installedVersion = findBrokenIntelGen7GraphicsDriver();

            if (installedVersion != null) {
                showUnsupportedDriverMessageBox(
                        INTEL_GEN7_DRIVER_MESSAGE
                                .replace("###CURRENT_DRIVER###", installedVersion.getFriendlyString()),
                        INTEL_GEN7_DRIVER_HELP_URL);
            }
        }
    }

    private static void showUnsupportedDriverMessageBox(String message, String url) {
        // Always print the information to the log file first, just in case we can't show the message box.
        LOGGER.error(CONSOLE_MESSAGE_TEMPLATE
                .replace("###ERROR_DESCRIPTION###", message)
                .replace("###HELP_URL###", url));

        // Try to show a graphical message box (if the platform supports it) and shut down the game.
        MessageBox.showMessageBox(null, MessageBox.IconType.ERROR, "Sodium Renderer - Unsupported Driver", message, url);
        System.exit(1 /* failure code */);
    }

    /**
     * Scans the detected graphics adapters to determine if a known-broken driver for Intel Gen7 GPUs is being used.
     * For context, see the original <a href="https://github.com/CaffeineMC/sodium-fabric/issues/899">bug report.</a>
     * @return The version string of a broken graphics driver, otherwise null
     */
    private static @Nullable WindowsDisplayDriverVersion findBrokenIntelGen7GraphicsDriver() {
        // Only affects Windows
        if (Util.getPlatform() != Util.OS.WINDOWS) {
            return null;
        }

        for (var adapter : GraphicsAdapterProbe.getAdapters()) {
            // Skip non-Intel adapters
            if (adapter.vendor() != GraphicsAdapterVendor.INTEL) {
                continue;
            }

            var version = WindowsDisplayDriverVersion.parse(adapter.version());

            if (version == null) {
                LOGGER.warn("Failed to parse graphics driver version: {}", version);
                continue;
            }

            // == *.10.18.10
            if (version.driverModel() == 10 && version.featureLevel() == 18 && version.major() == 10) {
                LOGGER.warn("Found matching driver version: {}", version);
                return version;
            }
        }

        return null;
    }
}
