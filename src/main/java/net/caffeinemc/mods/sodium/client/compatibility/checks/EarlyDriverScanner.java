package net.caffeinemc.mods.sodium.client.compatibility.checks;

import net.caffeinemc.mods.sodium.client.compatibility.environment.probe.GraphicsAdapterProbe;
import net.caffeinemc.mods.sodium.client.platform.MessageBox;
import net.caffeinemc.mods.sodium.client.platform.windows.WindowsDriverStoreVersion;
import net.caffeinemc.mods.sodium.client.platform.windows.api.d3dkmt.D3DKMT;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Performs OpenGL driver validation before the game creates an OpenGL context. This runs during the earliest possible
 * opportunity at game startup, and uses a custom hardware prober to search for problematic drivers.
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
            Required version: 10.18.10.5161 (or newer)
            
            You must update your graphics card driver in order to continue!
            
            Click the 'Help' button for more information on how you can do this.
            """;

    private static final String INTEL_GEN7_DRIVER_HELP_URL = "https://github.com/CaffeineMC/sodium-fabric/wiki/Driver-Compatibility#windows-intel-gen7";

    public static void scanDrivers() {
        if (Configuration.WIN32_DRIVER_INTEL_GEN7) {
            var installedVersion = findBrokenIntelGen7GraphicsDriver();

            if (installedVersion != null) {
                showUnsupportedDriverMessageBox(
                        INTEL_GEN7_DRIVER_MESSAGE
                                .replace("###CURRENT_DRIVER###", installedVersion.toString()),
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

    private static final String[] INTEL_GEN_7_GRAPHICS_DRIVER_NAMES = new String[] { "ig7icd64.dll", "ig7icd32.dll" };

    // https://github.com/CaffeineMC/sodium-fabric/issues/899
    private static @Nullable WindowsDriverStoreVersion findBrokenIntelGen7GraphicsDriver() {
        for (var adapter : GraphicsAdapterProbe.getAdapters()) {
            if (adapter instanceof D3DKMT.WDDMAdapterInfo d3dkmtAdapter) {
                var driverName = d3dkmtAdapter.openglIcdName();
                var driverVersion = d3dkmtAdapter.openglIcdVersion();

                if (driverName == null || driverVersion == null) {
                    continue;
                }

                if (ArrayUtils.contains(INTEL_GEN_7_GRAPHICS_DRIVER_NAMES, FilenameUtils.getName(driverName))) {
                    // https://www.intel.com/content/www/us/en/support/articles/000005654/graphics.html
                    if (driverVersion.major() == 10 && driverVersion.minor() < 5161) {
                        return driverVersion;
                    }
                }
            }
        }

        return null;
    }
}
