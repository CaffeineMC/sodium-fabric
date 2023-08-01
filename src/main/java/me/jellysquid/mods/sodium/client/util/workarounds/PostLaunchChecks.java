package me.jellysquid.mods.sodium.client.util.workarounds;

import me.jellysquid.mods.sodium.client.gui.console.Console;
import me.jellysquid.mods.sodium.client.gui.console.message.MessageLevel;
import me.jellysquid.mods.sodium.client.util.workarounds.driver.nvidia.NvidiaDriverVersion;
import me.jellysquid.mods.sodium.client.util.workarounds.platform.windows.WindowsDriverStoreVersion;
import me.jellysquid.mods.sodium.client.util.workarounds.probe.GraphicsAdapterProbe;
import me.jellysquid.mods.sodium.client.util.workarounds.probe.GraphicsAdapterVendor;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.Util.OperatingSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostLaunchChecks {
    private static final Logger LOGGER = LoggerFactory.getLogger("Sodium-PostlaunchChecks");

    public static void checkDrivers() {
        if (isBrokenNvidiaDriverInstalled()) {
            var message = Text.translatable("sodium.console.broken_nvidia_driver");

            Console.instance().logMessage(MessageLevel.ERROR, message, 45.0);
        }

        if (isUsingPojavLauncher()) {
            var message = Text.translatable("sodium.console.pojav_launcher");

            Console.instance().logMessage(MessageLevel.ERROR, message, 45.0);
        }
    }

    // https://github.com/CaffeineMC/sodium-fabric/issues/1486
    // The way which NVIDIA tries to detect the Minecraft process could not be circumvented until fairly recently
    // So we require that an up-to-date graphics driver is installed so that our workarounds can disable the Threaded
    // Optimizations driver hack.
    private static boolean isBrokenNvidiaDriverInstalled() {
        if (Util.getOperatingSystem() != OperatingSystem.WINDOWS) {
            return false;
        }

        for (var adapter : GraphicsAdapterProbe.getAdapters()) {
            if (adapter.vendor() != GraphicsAdapterVendor.NVIDIA) {
                continue;
            }

            try {
                var driverStoreVersion = WindowsDriverStoreVersion.parse(adapter.version());
                var nvidiaVersion = NvidiaDriverVersion.parse(driverStoreVersion);

                // Broken in 526.47
                // Fixed in 536.23
                if (nvidiaVersion.isWithinRange(new NvidiaDriverVersion(526, 47), new NvidiaDriverVersion(536, 23))) {
                    return true;
                }
            } catch (WindowsDriverStoreVersion.ParseException | NvidiaDriverVersion.ParseException ignored) { }
        }

        return false;
    }

    // https://github.com/CaffeineMC/sodium-fabric/issues/1916
    private static boolean isUsingPojavLauncher() {
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
