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

                // first broken version: 522.25
                if (nvidiaVersion.isOlderThan(new NvidiaDriverVersion(522, 25))) {
                    continue;
                }

                // last broken version: 535.98
                if (nvidiaVersion.isNewerThan(new NvidiaDriverVersion(535, 98))) {
                    continue;
                }

                return true;
            } catch (WindowsDriverStoreVersion.ParseException | NvidiaDriverVersion.ParseException ignored) { }
        }

        return false;
    }


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
