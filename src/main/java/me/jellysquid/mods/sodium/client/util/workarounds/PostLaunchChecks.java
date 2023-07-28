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

public class PostLaunchChecks {
    public static void checkWorkarounds() {
        var workarounds = Workarounds.getEnabledWorkarounds();

        if (!workarounds.isEmpty()) {
            Console.instance()
                    .logMessage(MessageLevel.WARN, Text.literal("One or more workarounds have been enabled to prevent problems."), 10.0);
            Console.instance()
                    .logMessage(MessageLevel.WARN, Text.literal(" * Performance may be reduced, and certain features may be disabled."), 10.0);
            Console.instance()
                    .logMessage(MessageLevel.WARN, Text.literal(" * Please see the log file for more information."), 10.0);
        }
    }

    public static void checkDrivers() {
        if (isBrokenNvidiaDriverInstalled()) {
            Console.instance()
                    .logMessage(MessageLevel.ERROR, Text.literal("Your NVIDIA graphics drivers are out of date!"), 45.0);
            Console.instance()
                    .logMessage(MessageLevel.ERROR, Text.literal(" * This will cause severe performance issues and crashes."), 45.0);
            Console.instance()
                    .logMessage(MessageLevel.ERROR, Text.literal(" * You must update your graphics drivers to the latest version."), 45.0);
            Console.instance()
                    .logMessage(MessageLevel.ERROR, Text.literal(" * See the log file for more information."), 45.0);
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
}
