package me.jellysquid.mods.sodium.client.util.workarounds;

import me.jellysquid.mods.sodium.client.util.workarounds.platform.windows.WindowsDriverStoreVersion;
import me.jellysquid.mods.sodium.client.util.workarounds.probe.GraphicsAdapterProbe;
import me.jellysquid.mods.sodium.client.util.workarounds.probe.GraphicsAdapterVendor;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreLaunchChecks {
    private static final Logger LOGGER = LoggerFactory.getLogger("Sodium-PrelaunchChecks");

    public static void checkDrivers() {
        boolean check = Boolean.parseBoolean(System.getProperty("sodium.driver.check", "true"));

        if (!check) {
            return;
        }

        if (isBrokenIntelGen7GraphicsDriver()) {
            LOGGER.error("------------------------------------------------------------------------------------------------------------");
            LOGGER.error("READ ME! You appear to be using an Intel graphics card with unsupported drivers!");
            LOGGER.error("  * Certain graphics cards (such as the Intel HD 2500/4000) currently ship with broken graphics drivers " +
                    "through Windows Update.");
            LOGGER.error("  * You need to update your graphics drivers to fix this problem. More instructions can be found here:");
            LOGGER.error("     https://github.com/CaffeineMC/sodium-fabric/issues/899");
            LOGGER.error("  * HINT: You cannot use Windows Update or the Intel Graphics Control Panel to install the updated graphics " +
                    "driver, as they incorrectly report that the driver is 'already up to date'.");

            LOGGER.error("  * HINT: If you believe this is an error, then you can force the game to start anyways by adding the " +
                    "following JVM argument.");
            LOGGER.error("      -Dsodium.driver.check=false");
            LOGGER.error("  * NOTE: We will not provide support for any issues caused by using this option. You are on your own!");
            LOGGER.error("------------------------------------------------------------------------------------------------------------");

            throw new RuntimeException("The currently installed Intel Graphics Driver is not compatible with Sodium, please " +
                    "see this issue for more details: https://github.com/CaffeineMC/sodium-fabric/issues/899");
        }
    }

    // https://github.com/CaffeineMC/sodium-fabric/issues/899
    private static boolean isBrokenIntelGen7GraphicsDriver() {
        if (Util.getOperatingSystem() != Util.OperatingSystem.WINDOWS) {
            return false;
        }

        for (var adapter : GraphicsAdapterProbe.getAdapters()) {
            if (adapter.vendor() != GraphicsAdapterVendor.INTEL) {
                continue;
            }

            try {
                var version = WindowsDriverStoreVersion.parse(adapter.version());

                if (version.driverModel() == 10 && version.featureLevel() == 18 && version.major() == 10) {
                    return true;
                }
            } catch (WindowsDriverStoreVersion.ParseException ignored) { }
        }

        return false;
    }
}
