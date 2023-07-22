package me.jellysquid.mods.sodium.client.util.workarounds;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GraphicsAdapterProbe {
    private static final Logger LOGGER = LoggerFactory.getLogger("Sodium-GraphicsAdapterProbe");

    public static List<Result> findAdapters() {
        LOGGER.info("Searching for graphics cards...");

        var systemInfo = new SystemInfo();
        var hardwareInfo = systemInfo.getHardware();

        var results = new ArrayList<Result>();

        for (var graphicsCard : hardwareInfo.getGraphicsCards()) {
            Vendor vendor = identifyVendorFromString(graphicsCard.getVendor());
            String name = graphicsCard.getName();

            var result = new Result(vendor, name);
            results.add(result);

            LOGGER.info("Found graphics card: Vendor={}, Name={}", graphicsCard.getVendor(), graphicsCard.getName());
        }

        return Collections.unmodifiableList(results);
    }


    @NotNull
    private static Vendor identifyVendorFromString(String vendor) {
        if (vendor.startsWith("Advanced Micro Devices, Inc.") || vendor.contains("(0x1002)")) {
            return Vendor.AMD;
        } else if (vendor.startsWith("NVIDIA") || vendor.contains("(0x10de)")) {
            return Vendor.NVIDIA;
        } else if (vendor.startsWith("Intel") || vendor.contains("(0x8086)")) {
            return Vendor.INTEL;
        }

        return Vendor.UNKNOWN;
    }

    public record Result(Vendor vendor, String name) {

    }

    public enum Vendor {
        NVIDIA,
        AMD,
        INTEL,
        UNKNOWN
    }
}
