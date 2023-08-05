package me.jellysquid.mods.sodium.client.util.workarounds.driver.nvidia;

import me.jellysquid.mods.sodium.client.util.workarounds.GLContextInfo;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.regex.Pattern;

public record NvidiaGLContextInfo(int major, int minor) {
    private static final Pattern PATTERN = Pattern.compile("^.*NVIDIA (?<major>\\d+)\\.(?<minor>\\d+)$");

    @Nullable
    public static NvidiaGLContextInfo tryParse(GLContextInfo driver) {
        if (!Objects.equals(driver.vendor(), "NVIDIA Corporation")) {
            return null;
        }

        var matcher = PATTERN.matcher(driver.version());

        if (!matcher.matches()) {
            return null;
        }

        int major, minor;

        try {
            major = Integer.parseInt(matcher.group("major"));
            minor = Integer.parseInt(matcher.group("minor"));
        } catch (NumberFormatException e) {
            return null;
        }

        return new NvidiaGLContextInfo(major, minor);
    }

    /**
     * @param oldest The oldest version (inclusive) to test against
     * @param newest The newest version (exclusive) to test against
     * @return True if this version is within the specified version range
     */
    public boolean isWithinRange(NvidiaGLContextInfo oldest, NvidiaGLContextInfo newest) {
        // Fail when (this < oldest)
        if (this.major < oldest.major || this.minor < oldest.minor) {
            return false;
        }

        // Fail when (this >= newest)
        if (this.major >= newest.major && this.minor >= newest.minor) {
            return false;
        }

        // Succeed when (this >= oldest) and (this < newest)
        return true;
    }
}
