package net.caffeinemc.mods.sodium.client.compatibility.workarounds.nvidia;

import net.caffeinemc.mods.sodium.client.compatibility.environment.GLContextInfo;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * The parsed version information from the proprietary NVIDIA Graphics Driver.
 */
public record NvidiaDriverVersion(int major, int minor) {
    private static final Pattern PATTERN = Pattern.compile("^.*NVIDIA (?<major>\\d+)\\.(?<minor>\\d+)(?<suffix>\\.\\d+)?$");

    /**
     * Attempts to parse information from the OpenGL context to obtain a version string for the NVIDIA Graphics
     * Driver. If the context does not correspond to a NVIDIA implementation, then <pre>null</pre> is always returned.
     *
     * @param contextInfo The identifying strings from the context implementation
     * @return The parsed version information, or null if it cannot be parsed
     */
    @Nullable
    public static NvidiaDriverVersion tryParse(GLContextInfo contextInfo) {
        if (!Objects.equals(contextInfo.vendor(), "NVIDIA Corporation")) {
            return null;
        }

        var matcher = PATTERN.matcher(contextInfo.version());

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

        return new NvidiaDriverVersion(major, minor);
    }

    /**
     * @param oldest The oldest version (inclusive) to test against
     * @param newest The newest version (exclusive) to test against
     * @return True if this version is within the specified version range
     */
    public boolean isWithinRange(NvidiaDriverVersion oldest, NvidiaDriverVersion newest) {
        return this.asInteger() >= oldest.asInteger() && this.asInteger() < newest.asInteger();
    }

    private long asInteger() {
        Validate.isTrue(this.major >= 0);
        Validate.isTrue(this.minor >= 0);

        return (Integer.toUnsignedLong(this.major) << 32) | (Integer.toUnsignedLong(this.minor) << 0);
    }
}
