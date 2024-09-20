package net.caffeinemc.mods.sodium.client.compatibility.workarounds.nvidia;

import net.caffeinemc.mods.sodium.client.platform.windows.WindowsFileVersion;

public record NvidiaDriverVersion(int major, int minor) {
    public static NvidiaDriverVersion parse(WindowsFileVersion version) {
        // NVIDIA drivers use a strange versioning format, where the major/minor are concatenated into
        // the end of the file version. For example, driver 526.47 is represented as X.Y.15.2657, where
        // the X and Y values are the usual for WDDM drivers.
        int merged = (((version.z() - 10) * 10_000) + version.w());
        int major = merged / 100;
        int minor = merged % 100;

        return new NvidiaDriverVersion(major, minor);
    }

    @Override
    public String toString() {
        return "%d.%d".formatted(this.major, this.minor);
    }
}
