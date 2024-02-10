package net.caffeinemc.mods.sodium.client.compatibility.environment.probe;

import org.jetbrains.annotations.NotNull;

/**
 * Known vendor types of graphics adapters. This is not a comprehensive enumeration, and it only contains vendors
 * we are interested in identifying for the purpose of applying workarounds.
 */
public enum GraphicsAdapterVendor {
    NVIDIA,
    AMD,
    INTEL,
    UNKNOWN;

    /**
     * <p>Tries to determine the vendor from an identifying string. The string may either be the Vendor ID belonging to
     * the PCI device, or the friendly name of the vendor as returned by the operating system.</p>
     *
     * <p>If the vendor cannot be identified, the default value of {@link GraphicsAdapterVendor#UNKNOWN} will be
     * returned.</p>
     *
     * TODO: This should be split into separate functions for the two input types (Vendor ID, Vendor Name).
     *
     * @param vendor The string which identifies the vendor
     * @return An enum constant of a known graphics adapter vendor
     */
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
