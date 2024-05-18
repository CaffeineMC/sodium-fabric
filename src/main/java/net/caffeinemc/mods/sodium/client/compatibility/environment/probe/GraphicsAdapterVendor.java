package net.caffeinemc.mods.sodium.client.compatibility.environment.probe;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.file.PathUtils;
import org.jetbrains.annotations.NotNull;

public enum GraphicsAdapterVendor {
    NVIDIA,
    AMD,
    INTEL,
    UNKNOWN;

    @NotNull
    static GraphicsAdapterVendor fromPciVendorId(String vendor) {
        if (vendor.contains("0x1002")) {
            return AMD;
        } else if (vendor.contains("0x10de")) {
            return NVIDIA;
        } else if (vendor.contains("0x8086")) {
            return INTEL;
        }

        return UNKNOWN;
    }

    public static GraphicsAdapterVendor fromIcdName(String name) {
        if (name.matches("ig(7|8|9|11|xelp|xehp)icd(32|64)")) {
            return INTEL;
        }

        if (name.matches("nvoglv(32|64)")) {
            return NVIDIA;
        }

        if (name.matches("atiglpxx|atig6pxx")) {
            return AMD;
        }

        return UNKNOWN;
    }
}
