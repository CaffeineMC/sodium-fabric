package net.caffeinemc.mods.sodium.client.compatibility.environment.probe;

import org.jetbrains.annotations.NotNull;

public enum GraphicsAdapterVendor {
    NVIDIA,
    AMD,
    INTEL,
    UNKNOWN;

    @NotNull
    static GraphicsAdapterVendor identifyVendorFromString(String vendor) {
        if (vendor.startsWith("Advanced Micro Devices, Inc.") || vendor.contains("0x1002")) {
            return AMD;
        } else if (vendor.startsWith("NVIDIA") || vendor.contains("0x10de")) {
            return NVIDIA;
        } else if (vendor.startsWith("Intel") || vendor.contains("0x8086")) {
            return INTEL;
        }

        return UNKNOWN;
    }
}
