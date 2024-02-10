package net.caffeinemc.mods.sodium.client.platform.windows;

import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

/**
 * <p>See also: <a href="https://learn.microsoft.com/en-us/windows-hardware/drivers/display/driver-dll-for-display-adapter-or-chipset-has-properly-formatted-file-version">
 *     Microsoft Documentation - File Version Formatting for Display Driver DLLs</a></p>
 */
public record WindowsDisplayDriverVersion(
        /* The Windows Display Driver Model (WDDM) version which the driver implements. */
        int driverModel,
        /* The DirectX feature level supported by the driver. For WDDM 1.1 and earlier, this is the DDI version instead. */
        int featureLevel,
        /* The major version of the driver. */
        int major,
        /* The minor version of the driver. */
        int minor)
{
    private static final Pattern PATTERN = Pattern.compile(
            "^(?<driverModel>[0-9]{1,2})\\.(?<featureLevel>[0-9]{1,2})\\.(?<major>[0-9]{1,5})\\.(?<minor>[0-9]{1,5})$");

    /**
     * Attempts to parse the version string into individual components according to the display driver version format.
     *
     * @param version The version string to parse
     * @return The parsed components of the version string, or null if it could not be parsed
     */
    public static @Nullable WindowsDisplayDriverVersion parse(String version) {
        var matcher = PATTERN.matcher(version);

        if (!matcher.matches()) {
            return null;
        }

        var driverModel = Integer.parseInt(matcher.group("driverModel"));
        var featureLevel = Integer.parseInt(matcher.group("featureLevel"));
        var major = Integer.parseInt(matcher.group("major"));
        var minor = Integer.parseInt(matcher.group("minor"));

        return new WindowsDisplayDriverVersion(driverModel, featureLevel, major, minor);
    }

    /**
     * @return The human-readable version string of the driver
     */
    public String getFriendlyString() {
        return "%s.%s.%s.%s".formatted(this.driverModel, this.featureLevel, this.major, this.minor);
    }
}
