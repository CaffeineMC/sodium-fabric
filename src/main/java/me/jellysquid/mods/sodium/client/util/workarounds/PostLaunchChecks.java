package me.jellysquid.mods.sodium.client.util.workarounds;

import me.jellysquid.mods.sodium.client.gui.console.Console;
import me.jellysquid.mods.sodium.client.gui.console.message.MessageLevel;
import me.jellysquid.mods.sodium.client.util.workarounds.driver.nvidia.NvidiaDriverVersion;
import me.jellysquid.mods.sodium.client.util.workarounds.platform.windows.WindowsDriverStoreVersion;
import me.jellysquid.mods.sodium.client.util.workarounds.probe.GraphicsAdapterProbe;
import me.jellysquid.mods.sodium.client.util.workarounds.probe.GraphicsAdapterVendor;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.Util.OperatingSystem;

public class PostLaunchChecks {
    public static void checkDrivers() {
        if (isBrokenNvidiaDriverInstalled()) {
            var message = Text.literal("""
                    Your NVIDIA graphics drivers are out of date!
                      * This will cause severe performance issues and crashes.
                      * Please update your graphics drivers to the latest version.
                    (Read the log file for more information.)""");

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
}
