package me.jellysquid.mods.sodium.client.util.workarounds;

import me.jellysquid.mods.sodium.client.util.workarounds.platform.windows.WindowsDriverStoreVersion;
import me.jellysquid.mods.sodium.client.util.workarounds.probe.GraphicsAdapterProbe;
import me.jellysquid.mods.sodium.client.util.workarounds.probe.GraphicsAdapterVendor;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreLaunchChecks {
    private static final Logger LOGGER = LoggerFactory.getLogger("Sodium-PrelaunchChecks");

    private static boolean isEnabled(String key) {
        return Boolean.parseBoolean(System.getProperty(key, "true"));
    }

    public static void checkPlatform() {
        boolean check = isEnabled("sodium.platform.check");

        if (!check) {
            return;
        }

        if (isRunningOnAndroid()) {
            LOGGER.error("------------------------------------------------------------------------------------------------------------");
            LOGGER.error("READ ME! You appear to be using an Android device!");
            LOGGER.error("  * Most Android devices and OpenGL compatibility layers are not compatible with Sodium, and will cause severe " +
                    "graphical issues and crashes. These problems are not caused by Sodium.");
            LOGGER.error("  * By default, the game will not start if it detects an Android device is being used, since it almost " +
                    "never works correctly, and results in users submitting many invalid bug reports.");
            LOGGER.error("  * For more information, please see the following issue:");
            LOGGER.error("      https://github.com/CaffeineMC/sodium-fabric/issues/1916");
            LOGGER.error("  * HINT: If you are absolutely sure that you know what you are doing (i.e. you are a developer working on the " +
                    "code), then you can force the game to start anyways by adding the following JVM argument.");
            LOGGER.error("      -Dsodium.platform.check=false");
            LOGGER.error("  * NOTE: We will not provide support for any issues caused by using this option. You are on your own!");
            LOGGER.error("------------------------------------------------------------------------------------------------------------");

            throw new RuntimeException("Android is not supported when using Sodium, please " +
                    "see this issue for more details: https://github.com/CaffeineMC/sodium-fabric/issues/1916");
        }
    }

    public static void checkDrivers() {
        boolean check = isEnabled("sodium.driver.check");

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

                if (version.equals(new WindowsDriverStoreVersion(10, 18, 10, 4538))) {
                    return true;
                }
            } catch (WindowsDriverStoreVersion.ParseException ignored) { }
        }

        return false;
    }

    private static boolean isRunningOnAndroid() {
        if (System.getenv("POJAV_RENDERER") != null) {
            LOGGER.warn("Detected presence of environment variable POJAV_LAUNCHER, which seems to indicate we are running on Android");

            return true;
        }

        var librarySearchPaths = System.getProperty("java.library.path", null);

        if (librarySearchPaths != null) {
            for (var path : librarySearchPaths.split(":")) {
                if (isKnownAndroidPathFragment(path)) {
                    LOGGER.warn("Found a library search path which seems to be hosted in an Android filesystem: {}", path);

                    return true;
                }
            }
        }

        var workingDirectory = System.getProperty("user.home", null);

        if (workingDirectory != null) {
            if (isKnownAndroidPathFragment(workingDirectory)) {
                LOGGER.warn("Working directory seems to be hosted in an Android filesystem: {}", workingDirectory);
            }
        }

        return false;
    }

    private static boolean isKnownAndroidPathFragment(String path) {
        return path.matches("/data/user/[0-9]+/net\\.kdt\\.pojavlaunch");
    }
}
