package net.caffeinemc.mods.sodium.client.compatibility.environment.probe;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;
import oshi.util.ExecutingCommand;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.minecraft.Util;

/**
 * Utility class which provides functions for scanning the system's attached graphics adapters.
 */
public class GraphicsAdapterProbe {
    private static final Logger LOGGER = LoggerFactory.getLogger("Sodium-GraphicsAdapterProbe");

    private static @Nullable List<GraphicsAdapterInfo> ADAPTERS;

    /**
     * Searches the system for graphics adapters using the best strategy for the given platform.
     */
    public static void scanAdapters() {
        LOGGER.info("Searching for graphics cards...");

        // We rely on separate detection logic for Linux because Oshi fails to find GPUs without
        // display outputs, and we can also retrieve the driver version for NVIDIA GPUs this way.
        var results = Util.getPlatform() == Util.OS.LINUX
                ? findAdaptersLinux()
                : findAdaptersCrossPlatform();

        if (results.isEmpty()) {
            LOGGER.warn("No graphics cards were found. Either you have no hardware devices supporting 3D acceleration, or " +
                    "something has gone terribly wrong!");
        }

        ADAPTERS = results;
    }

    /**
     * Scans the system for graphics adapters using a cross-platform library.
     * @return A list of graphics adapters
     */
    public static List<GraphicsAdapterInfo> findAdaptersCrossPlatform() {
        var systemInfo = new SystemInfo();
        var hardwareInfo = systemInfo.getHardware();

        var results = new ArrayList<GraphicsAdapterInfo>();

        for (var graphicsCard : hardwareInfo.getGraphicsCards()) {
            GraphicsAdapterVendor vendor = GraphicsAdapterVendor.identifyVendorFromString(graphicsCard.getVendor());
            String name = graphicsCard.getName();
            String versionInfo = graphicsCard.getVersionInfo();

            var info = new GraphicsAdapterInfo(vendor, name, versionInfo);
            results.add(info);

            LOGGER.info("Found graphics card: {}", info);
        }

        return results;
    }

    /**
     * Scans the system for graphics adapters using the sysfs interface on Linux. Unlike the cross-platform
     * implementation, this will return results for graphics adapters with no display outputs, which can be found on
     * certain laptops with hybrid graphics.
     * @return A list of graphics adapters
     */
    private static List<GraphicsAdapterInfo> findAdaptersLinux() {
        var results = new ArrayList<GraphicsAdapterInfo>();

        try (var devices = Files.list(Path.of("/sys/bus/pci/devices/"))) {
            Iterable<Path> devicesIter = devices::iterator;

            for (var devicePath : devicesIter) {
                // 0x030000 = VGA compatible controller
                // 0x030200 = 3D controller (GPUs with no inputs attached, e.g. hybrid graphics laptops)
                var deviceClass = Files.readString(devicePath.resolve("class")).trim();
                if (!deviceClass.equals("0x030000") && !deviceClass.equals("0x030200")) {
                    continue;
                }

                var deviceVendor = Files.readString(devicePath.resolve("vendor")).trim();
                GraphicsAdapterVendor vendor = GraphicsAdapterVendor.identifyVendorFromString(deviceVendor);

                // The Linux kernel doesn't provide a way to get the device name, so we need to use lspci,
                // since it comes with a list of known device names mapped to device IDs.
                var deviceId = Files.readString(devicePath.resolve("device")).trim();
                var name = ExecutingCommand // See `man lspci` for more information
                        .runNative("lspci -vmm -d " + deviceVendor.substring(2) + ":" + deviceId.substring(2))
                        .stream()
                        .filter(line -> line.startsWith("Device:"))
                        .map(line -> line.substring("Device:".length()).trim())
                        .findFirst()
                        .orElse("unknown");

                // This works for the NVIDIA driver, not for i915/amdgpu/etc. though (for obvious reasons).
                var versionInfo = "unknown";
                try {
                    versionInfo = Files.readString(devicePath.resolve("driver/module/version")).trim();
                } catch (IOException ignored) {}

                var info = new GraphicsAdapterInfo(vendor, name, versionInfo);
                results.add(info);

                LOGGER.info("Found graphics card: {}", info);
            }
        } catch (IOException ignored) {}

        return results;
    }

    /**
     * @return A collection of graphics adapters which were found during a hardware scan
     * @throws IllegalStateException If the function is called before {@link GraphicsAdapterProbe#scanAdapters()}
     */
    public static Collection<GraphicsAdapterInfo> getAdapters() {
        var adapters = ADAPTERS;

        if (adapters == null) {
            throw new IllegalStateException("Graphics adapters not probed yet");
        }

        return adapters;
    }
}
