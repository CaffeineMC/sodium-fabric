package net.caffeinemc.mods.sodium.client.compatibility.environment.probe;

import net.caffeinemc.mods.sodium.client.compatibility.environment.OsUtils;
import net.caffeinemc.mods.sodium.client.platform.windows.api.d3dkmt.D3DKMT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.util.ExecutingCommand;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class GraphicsAdapterProbe {
    private static final Logger LOGGER = LoggerFactory.getLogger("Sodium-GraphicsAdapterProbe");

    private static List<? extends GraphicsAdapterInfo> ADAPTERS = List.of();

    public static void findAdapters() {
        LOGGER.info("Searching for graphics cards...");

        List<? extends GraphicsAdapterInfo> adapters;

        try {
            adapters = switch (OsUtils.getOs()) {
                case WIN -> findAdapters$Windows();
                case LINUX -> findAdapters$Linux();
                default -> null;
            };
        } catch (Exception e) {
            LOGGER.error("Failed to find graphics adapters!", e);
            return;
        }

        if (adapters == null) {
            // Not supported on this platform
            return;
        } else if (adapters.isEmpty()) {
            // Tried to search for adapters, but didn't find anything
            LOGGER.warn("Could not find any graphics adapters! Probably the device is not on a bus we can probe, or " +
                    "there are no devices supporting 3D acceleration.");
        } else {
            // Search returned some adapters
            for (var adapter : adapters) {
                LOGGER.info("Found graphics adapter: {}", adapter);
            }
        }

        ADAPTERS = adapters;
    }

    private static List<? extends GraphicsAdapterInfo> findAdapters$Windows() {
        return D3DKMT.findGraphicsAdapters();
    }

    // We rely on separate detection logic for Linux because Oshi fails to find GPUs without
    // display outputs, and we can also retrieve the driver version for NVIDIA GPUs this way.
    private static List<? extends GraphicsAdapterInfo> findAdapters$Linux() {
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

                var pciVendorId = Files.readString(devicePath.resolve("vendor")).trim();
                var pciDeviceId = Files.readString(devicePath.resolve("device")).trim();


                // The Linux kernel doesn't provide a way to get the device name, so we need to use lspci,
                // since it comes with a list of known device names mapped to device IDs.
                var name = ExecutingCommand // See `man lspci` for more information
                        .runNative("lspci -vmm -d " + pciVendorId.substring(2) + ":" + pciDeviceId.substring(2))
                        .stream()
                        .filter(line -> line.startsWith("Device:"))
                        .map(line -> line.substring("Device:".length()).trim())
                        .findFirst()
                        .orElse("unknown");

                var vendor = GraphicsAdapterVendor.fromPciVendorId(pciVendorId);

                var info = new GraphicsAdapterInfo.LinuxPciAdapterInfo(vendor, name, pciVendorId, pciDeviceId);
                results.add(info);
            }
        } catch (IOException ignored) {}

        return results;
    }

    public static Collection<? extends GraphicsAdapterInfo> getAdapters() {
        if (ADAPTERS == null) {
            LOGGER.error("Graphics adapters not probed yet; returning an empty list.");
            return Collections.emptyList();
        }

        return ADAPTERS;
    }
}
